package com.mov.service;

import com.mov.model.Grupo;
import com.mov.model.SalaDebate;
import com.mov.model.Usuario;
import com.mov.model.enums.MomentoSimulacao;
import com.mov.model.enums.TipoEventoLog;
import com.mov.model.enums.TipoUsuario;
import com.mov.repository.GrupoRepository;
import com.mov.repository.SalaDebateRepository;
import com.mov.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalaDebateRepository salaDebateRepository;
    private final TelemetriaService telemetriaService;

    public List<Grupo> listarTodosAtivos() {
        return grupoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    public List<Grupo> listarGruposVisiveisParaUsuario(Usuario usuario) {
        if (usuario == null) return List.of();
        if (isEquipeOrganizacao(usuario) || usuario.getTipo() == TipoUsuario.MESA_DIRETORA) {
            return grupoRepository.findByAtivoTrueOrderByNomeAsc();
        }
        return grupoRepository.findGruposDoUsuario(usuario.getId());
    }

    public Grupo buscarPorIdComPermissao(UUID grupoId, Usuario solicitante) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado."));

        if (!temPermissaoAcesso(solicitante, grupo)) {
            telemetriaService.registrarEvento(
                    TipoEventoLog.ACESSO_INDEVIDO_BLOQUEADO,
                    solicitante,
                    grupo,
                    grupoId,
                    "Tentativa de acesso bloqueada ao grupo: " + grupo.getNome()
            );
            throw new SecurityException("Acesso negado: você não pertence a este grupo/delegação.");
        }

        return grupo;
    }

    public boolean temPermissaoAcesso(Usuario usuario, Grupo grupo) {
        if (usuario == null || grupo == null) return false;
        if (isEquipeOrganizacao(usuario) || usuario.getTipo() == TipoUsuario.MESA_DIRETORA) {
            return true;
        }
        return grupo.contemIntegrante(usuario.getId());
    }

    public boolean temPermissaoEdicao(Usuario usuario, Grupo grupo) {
        if (usuario == null || grupo == null) return false;
        if (telemetriaService.getMomentoAtual() == MomentoSimulacao.DEPOIS_EVENTO && !isEquipeOrganizacao(usuario)) {
            return false; // Após o evento, registros são preservados para consulta (Seção 5.3)
        }
        if (isEquipeOrganizacao(usuario)) {
            return true;
        }
        return grupo.contemIntegrante(usuario.getId());
    }

    public boolean isEquipeOrganizacao(Usuario usuario) {
        if (usuario == null || usuario.getTipo() == null) return false;
        return usuario.getTipo() == TipoUsuario.ADMIN || usuario.getTipo() == TipoUsuario.PROFESSOR;
    }

    @Transactional
    public Grupo criarGrupo(String nome, String paisDelegacao, String sigla, String descricao, UUID salaId, Usuario criador) {
        if (!isEquipeOrganizacao(criador)) {
            throw new SecurityException("Apenas a equipe de organização pode criar grupos.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do grupo é obrigatório.");
        }
        if (paisDelegacao == null || paisDelegacao.isBlank()) {
            paisDelegacao = nome;
        }

        SalaDebate sala = null;
        if (salaId != null) {
            sala = salaDebateRepository.findById(salaId).orElse(null);
        }

        Grupo grupo = Grupo.builder()
                .nome(nome.trim())
                .paisDelegacao(paisDelegacao.trim())
                .sigla(sigla != null ? sigla.trim().toUpperCase() : "")
                .descricao(descricao != null ? descricao.trim() : "")
                .textoColaborativo("# Documento de Posição — " + paisDelegacao.trim() + "\n\nRegistre aqui a síntese oficial e os pontos principais acordados pela delegação.")
                .salaDebate(sala)
                .criadoPor(criador)
                .ativo(true)
                .build();

        Grupo salvo = grupoRepository.save(grupo);
        telemetriaService.registrarEvento(TipoEventoLog.CRIACAO_GRUPO, criador, salvo, salvo.getId(), "Grupo criado: " + salvo.getNome());
        return salvo;
    }

    @Transactional
    public void adicionarIntegrante(UUID grupoId, UUID usuarioId, Usuario responsavel) {
        if (!isEquipeOrganizacao(responsavel)) {
            throw new SecurityException("Apenas a equipe de organização pode gerenciar integrantes dos grupos.");
        }
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado."));
        Usuario novoMembro = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Participante não encontrado."));

        grupo.getIntegrantes().add(novoMembro);
        grupoRepository.save(grupo);

        telemetriaService.registrarEvento(
                TipoEventoLog.MEMBRO_ADICIONADO,
                responsavel,
                grupo,
                novoMembro.getId(),
                "Integrante " + novoMembro.getNome() + " adicionado ao " + grupo.getNome()
        );
    }

    @Transactional
    public void removerIntegrante(UUID grupoId, UUID usuarioId, Usuario responsavel) {
        if (!isEquipeOrganizacao(responsavel)) {
            throw new SecurityException("Apenas a equipe de organização pode gerenciar integrantes dos grupos.");
        }
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado."));
        grupo.getIntegrantes().removeIf(u -> u.getId().equals(usuarioId));
        grupoRepository.save(grupo);
    }

    @Transactional
    public void adicionarMultiplosIntegrantes(UUID grupoId, List<UUID> usuarioIds, Usuario responsavel) {
        if (!isEquipeOrganizacao(responsavel)) {
            throw new SecurityException("Apenas a equipe de organização pode gerenciar integrantes dos grupos.");
        }
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um participante.");
        }
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado."));
        List<Usuario> novos = usuarioRepository.findAllById(usuarioIds);
        grupo.getIntegrantes().addAll(novos);
        grupoRepository.save(grupo);

        telemetriaService.registrarEvento(
                TipoEventoLog.MEMBRO_ADICIONADO,
                responsavel,
                grupo,
                grupo.getId(),
                novos.size() + " integrantes vinculados em lote ao " + grupo.getNome()
        );
    }

    @Transactional
    public void definirGrupoDoParticipante(UUID usuarioId, UUID novoGrupoId, Usuario responsavel) {
        if (!isEquipeOrganizacao(responsavel)) {
            throw new SecurityException("Apenas a equipe de organização pode alterar o grupo de um participante.");
        }
        Usuario participante = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Participante não encontrado."));

        // Remove de grupos anteriores
        List<Grupo> gruposAtuais = grupoRepository.findGruposDoUsuario(usuarioId);
        for (Grupo g : gruposAtuais) {
            g.getIntegrantes().removeIf(u -> u.getId().equals(usuarioId));
            grupoRepository.save(g);
        }

        // Adiciona ao novo grupo, se informado
        if (novoGrupoId != null) {
            Grupo novoGrupo = grupoRepository.findById(novoGrupoId)
                    .orElseThrow(() -> new IllegalArgumentException("Novo grupo não encontrado."));
            novoGrupo.getIntegrantes().add(participante);
            grupoRepository.save(novoGrupo);
        }
    }

    @Transactional
    public Grupo entrarNoGrupoAutoAtendimento(UUID grupoId, Usuario estudante) {
        if (estudante == null) {
            throw new SecurityException("Faça login para entrar na sua delegação.");
        }
        List<Grupo> jaPertence = grupoRepository.findGruposDoUsuario(estudante.getId());
        if (!jaPertence.isEmpty() && !isEquipeOrganizacao(estudante)) {
            throw new IllegalArgumentException("Você já está vinculado à delegação: " + jaPertence.get(0).getNome() + ". Caso precise trocar de país, solicite à equipe de organização.");
        }
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new IllegalArgumentException("Delegação não encontrada."));
        grupo.getIntegrantes().add(estudante);
        Grupo salvo = grupoRepository.save(grupo);

        telemetriaService.registrarEvento(
                TipoEventoLog.MEMBRO_ADICIONADO,
                estudante,
                salvo,
                estudante.getId(),
                "Estudante vinculou-se por autoatendimento ao " + salvo.getNome()
        );
        return salvo;
    }

    @Transactional
    public Grupo buscarOuCriarGrupoPorPais(String paisOuNome, Usuario criador) {
        String limpo = paisOuNome.trim();
        String nomePadrao = limpo.toLowerCase().startsWith("grupo") ? limpo : "Grupo — Delegação " + limpo;

        for (Grupo existente : grupoRepository.findByAtivoTrueOrderByNomeAsc()) {
            if (existente.getNome().equalsIgnoreCase(limpo)
                    || existente.getNome().equalsIgnoreCase(nomePadrao)
                    || existente.getPaisDelegacao().equalsIgnoreCase(limpo)) {
                return existente;
            }
        }
        String sigla = limpo.length() >= 3 ? limpo.substring(0, 3).toUpperCase() : limpo.toUpperCase();
        return criarGrupo(
                nomePadrao,
                limpo,
                sigla,
                "Espaço oficial da Delegação (" + limpo + ") para documentos e arquivo compartilhado de argumentos.",
                null,
                criador
        );
    }

    @Transactional
    public Grupo atualizarTextoColaborativo(UUID grupoId, String novoTexto, Usuario editor) {
        Grupo grupo = buscarPorIdComPermissao(grupoId, editor);
        if (!temPermissaoEdicao(editor, grupo)) {
            throw new SecurityException("Você não possui permissão para editar o arquivo deste grupo no momento atual.");
        }
        grupo.setTextoColaborativo(novoTexto);
        grupo.setUltimoEditorTextoNome(editor.getNome());
        grupo.setDataAtualizacaoTexto(LocalDateTime.now());
        Grupo salvo = grupoRepository.save(grupo);

        telemetriaService.registrarEvento(
                TipoEventoLog.EDICAO_TEXTO_GRUPO,
                editor,
                salvo,
                salvo.getId(),
                "Documento colaborativo do grupo atualizado por " + editor.getNome()
        );
        return salvo;
    }
}
