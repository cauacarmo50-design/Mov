package com.mov.service;

import com.mov.model.Documento;
import com.mov.model.Grupo;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoDocumento;
import com.mov.model.enums.TipoEventoLog;
import com.mov.repository.DocumentoRepository;
import com.mov.repository.GrupoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoService grupoService;
    private final FileStorageService fileStorageService;
    private final TelemetriaService telemetriaService;

    public List<Documento> listarDocumentosGerais() {
        return documentoRepository.findByPublicoGeralTrueOrderByDataEnvioDesc();
    }

    public List<Documento> listarTodosParaAdmin() {
        return documentoRepository.findAllByOrderByDataEnvioDesc();
    }

    public List<Documento> listarDocumentosDoGrupo(UUID grupoId, Usuario solicitante) {
        Grupo grupo = grupoService.buscarPorIdComPermissao(grupoId, solicitante);
        return documentoRepository.findByGrupoIdOrderByDataEnvioDesc(grupo.getId());
    }

    public List<Documento> listarDocumentosAcessiveisPeloUsuario(Usuario usuario) {
        if (usuario == null) return listarDocumentosGerais();
        if (grupoService.isEquipeOrganizacao(usuario)) {
            return documentoRepository.findAllByOrderByDataEnvioDesc();
        }
        List<Documento> resultado = new ArrayList<>(listarDocumentosGerais());
        List<Grupo> meusGrupos = grupoService.listarGruposVisiveisParaUsuario(usuario);
        for (Grupo g : meusGrupos) {
            for (Documento docGrupo : documentoRepository.findByGrupoIdOrderByDataEnvioDesc(g.getId())) {
                if (resultado.stream().noneMatch(d -> d.getId().equals(docGrupo.getId()))) {
                    resultado.add(docGrupo);
                }
            }
        }
        return resultado;
    }

    @Transactional
    public Documento enviarDocumento(
            MultipartFile arquivo,
            String titulo,
            String descricao,
            TipoDocumento tipoDocumento,
            UUID grupoId,
            boolean publicoGeral,
            Usuario autor
    ) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("O título do documento é obrigatório.");
        }

        Grupo grupo = null;
        if (grupoId != null) {
            grupo = grupoService.buscarPorIdComPermissao(grupoId, autor);
            if (!grupoService.temPermissaoEdicao(autor, grupo)) {
                throw new SecurityException("Sem permissão para inserir documentos neste grupo.");
            }
        } else {
            // Documento sem grupo (geral da simulação): apenas equipe ou permitido se publicoGeral
            if (!grupoService.isEquipeOrganizacao(autor)) {
                List<Grupo> gruposDoUsuario = grupoService.listarGruposVisiveisParaUsuario(autor);
                if (!gruposDoUsuario.isEmpty()) {
                    grupo = gruposDoUsuario.get(0);
                    publicoGeral = false;
                } else {
                    throw new SecurityException("Apenas a organização pode publicar documentos gerais sem vínculo de delegação.");
                }
            } else {
                publicoGeral = true;
            }
        }

        String nomeSalvo = fileStorageService.storeFile(arquivo);
        String nomeOriginal = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "documento.pdf";

        Documento doc = Documento.builder()
                .titulo(titulo.trim())
                .descricao(descricao != null ? descricao.trim() : "")
                .tipoDocumento(tipoDocumento != null ? tipoDocumento : TipoDocumento.PESQUISA)
                .nomeOriginal(nomeOriginal)
                .nomeArmazenado(nomeSalvo)
                .contentType(arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream")
                .tamanhoBytes(arquivo.getSize())
                .publicoGeral(publicoGeral)
                .somenteLeitura(grupoService.isEquipeOrganizacao(autor))
                .grupo(grupo)
                .enviadoPor(autor)
                .totalConsultas(0)
                .build();

        Documento salvo = documentoRepository.save(doc);
        telemetriaService.registrarEvento(
                TipoEventoLog.UPLOAD_DOCUMENTO,
                autor,
                grupo,
                salvo.getId(),
                "Documento inserido: " + salvo.getTitulo() + " (" + salvo.getNomeOriginal() + ")"
        );
        return salvo;
    }

    @Transactional
    public Documento obterDocumentoParaAcesso(UUID documentoId, Usuario solicitante, boolean isDownload) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        if (!Boolean.TRUE.equals(doc.getPublicoGeral()) && doc.getGrupo() != null) {
            if (!grupoService.temPermissaoAcesso(solicitante, doc.getGrupo())) {
                telemetriaService.registrarEvento(
                        TipoEventoLog.ACESSO_INDEVIDO_BLOQUEADO,
                        solicitante,
                        doc.getGrupo(),
                        doc.getId(),
                        "Tentativa de acesso indevido ao documento: " + doc.getTitulo()
                );
                throw new SecurityException("Você não tem permissão para acessar documentos exclusivos de outra delegação.");
            }
        }

        doc.setTotalConsultas((doc.getTotalConsultas() != null ? doc.getTotalConsultas() : 0) + 1);
        documentoRepository.save(doc);

        telemetriaService.registrarEvento(
                isDownload ? TipoEventoLog.DOWNLOAD_DOCUMENTO : TipoEventoLog.VISUALIZACAO_DOCUMENTO,
                solicitante,
                doc.getGrupo(),
                doc.getId(),
                (isDownload ? "Download" : "Visualização") + " do documento: " + doc.getTitulo()
        );
        return doc;
    }

    public Resource carregarRecursoArquivo(Documento doc) {
        return fileStorageService.loadFileAsResource(doc.getNomeArmazenado());
    }

    @Transactional
    public void excluirDocumento(UUID documentoId, Usuario solicitante) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));

        boolean isAdmin = grupoService.isEquipeOrganizacao(solicitante);
        boolean isAutor = doc.getEnviadoPor() != null && solicitante != null && doc.getEnviadoPor().getId().equals(solicitante.getId());

        if (!isAdmin && !isAutor) {
            throw new SecurityException("Apenas o autor do envio ou a equipe de organização pode remover este documento.");
        }
        documentoRepository.delete(doc);
    }
}
