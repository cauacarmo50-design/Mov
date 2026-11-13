package com.mov.service;

import com.mov.model.ArgumentoRegistro;
import com.mov.model.Grupo;
import com.mov.model.HistoricoArgumento;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoEventoLog;
import com.mov.repository.ArgumentoRegistroRepository;
import com.mov.repository.HistoricoArgumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArgumentoService {

    private final ArgumentoRegistroRepository argumentoRepository;
    private final HistoricoArgumentoRepository historicoRepository;
    private final GrupoService grupoService;
    private final TelemetriaService telemetriaService;
    private final SimpMessagingTemplate messagingTemplate;

    public List<ArgumentoRegistro> listarPorGrupo(UUID grupoId, Usuario solicitante) {
        Grupo grupo = grupoService.buscarPorIdComPermissao(grupoId, solicitante);
        return argumentoRepository.findByGrupoIdOrderByDataAtualizacaoDesc(grupo.getId());
    }

    public List<ArgumentoRegistro> listarCompartilhadosPlenario() {
        return argumentoRepository.findByCompartilhadoPlenarioTrueOrderByDataAtualizacaoDesc();
    }

    public List<ArgumentoRegistro> listarTodosAdmin() {
        return argumentoRepository.findAllByOrderByDataAtualizacaoDesc();
    }

    public List<HistoricoArgumento> listarHistoricoDoArgumento(UUID argumentoId, Usuario solicitante) {
        ArgumentoRegistro reg = argumentoRepository.findById(argumentoId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de argumento não encontrado."));
        if (reg.getGrupo() != null && !Boolean.TRUE.equals(reg.getCompartilhadoPlenario())) {
            grupoService.buscarPorIdComPermissao(reg.getGrupo().getId(), solicitante);
        }
        return historicoRepository.findByArgumentoIdOrderByNumeroVersaoDesc(argumentoId);
    }

    public List<HistoricoArgumento> listarHistoricoDoGrupo(UUID grupoId, Usuario solicitante) {
        Grupo grupo = grupoService.buscarPorIdComPermissao(grupoId, solicitante);
        return historicoRepository.findByGrupoIdOrderByDataAlteracaoDesc(grupo.getId());
    }

    @Transactional
    public ArgumentoRegistro registrarArgumento(
            UUID grupoId,
            String delegacao,
            String argumento,
            String justificativa,
            String fonte,
            String linkFonte,
            String contraponto,
            boolean compartilhadoPlenario,
            Usuario autor
    ) {
        if (argumento == null || argumento.isBlank()) {
            throw new IllegalArgumentException("O campo 'Argumento' é obrigatório.");
        }

        Grupo grupo = null;
        if (grupoId != null) {
            grupo = grupoService.buscarPorIdComPermissao(grupoId, autor);
            if (!grupoService.temPermissaoEdicao(autor, grupo)) {
                throw new SecurityException("Sem permissão para registrar argumentos neste grupo.");
            }
        } else {
            List<Grupo> grupos = grupoService.listarGruposVisiveisParaUsuario(autor);
            if (!grupos.isEmpty()) {
                grupo = grupos.get(0);
            }
        }

        String delegacaoFinal = (delegacao != null && !delegacao.isBlank())
                ? delegacao.trim()
                : (grupo != null ? grupo.getPaisDelegacao() : "Delegação Geral");

        ArgumentoRegistro novo = ArgumentoRegistro.builder()
                .grupo(grupo)
                .delegacao(delegacaoFinal)
                .argumento(argumento.trim())
                .justificativa(justificativa != null ? justificativa.trim() : "")
                .fonte(fonte != null ? fonte.trim() : "")
                .linkFonte(normalizarLink(linkFonte))
                .contraponto(contraponto != null ? contraponto.trim() : "")
                .autorContrapontoNome((contraponto != null && !contraponto.isBlank()) ? autor.getNome() : null)
                .delegacaoContraponto((contraponto != null && !contraponto.isBlank()) ? delegacaoFinal : null)
                .compartilhadoPlenario(compartilhadoPlenario)
                .momento(telemetriaService.getMomentoAtual())
                .autor(autor)
                .ultimoEditor(autor)
                .numeroVersao(1)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();

        ArgumentoRegistro salvo = argumentoRepository.save(novo);
        salvarSnapshotHistorico(salvo, "CRIAÇÃO", autor);

        telemetriaService.registrarEvento(
                TipoEventoLog.REGISTRO_ARGUMENTO,
                autor,
                grupo,
                salvo.getId(),
                "Argumento registrado pela delegação " + delegacaoFinal
        );

        notificarAtualizacaoTempoReal(salvo, "NOVO_ARGUMENTO");
        return salvo;
    }

    @Transactional
    public ArgumentoRegistro editarArgumento(
            UUID argumentoId,
            String delegacao,
            String argumento,
            String justificativa,
            String fonte,
            String linkFonte,
            String contraponto,
            boolean compartilhadoPlenario,
            Usuario editor
    ) {
        ArgumentoRegistro reg = argumentoRepository.findById(argumentoId)
                .orElseThrow(() -> new IllegalArgumentException("Argumento não encontrado."));

        if (reg.getGrupo() != null) {
            if (!grupoService.temPermissaoEdicao(editor, reg.getGrupo())) {
                throw new SecurityException("Você não possui permissão para editar argumentos de outra delegação.");
            }
        }

        if (argumento == null || argumento.isBlank()) {
            throw new IllegalArgumentException("O campo 'Argumento' não pode ficar vazio.");
        }

        if (delegacao != null && !delegacao.isBlank()) {
            reg.setDelegacao(delegacao.trim());
        }
        reg.setArgumento(argumento.trim());
        reg.setJustificativa(justificativa != null ? justificativa.trim() : "");
        reg.setFonte(fonte != null ? fonte.trim() : "");
        reg.setLinkFonte(normalizarLink(linkFonte));
        if (contraponto != null) {
            boolean mudouContraponto = !contraponto.trim().equals(reg.getContraponto() != null ? reg.getContraponto() : "");
            reg.setContraponto(contraponto.trim());
            if (mudouContraponto && !contraponto.isBlank()) {
                reg.setAutorContrapontoNome(editor.getNome());
            }
        }
        reg.setCompartilhadoPlenario(compartilhadoPlenario);
        reg.setUltimoEditor(editor);
        reg.setNumeroVersao((reg.getNumeroVersao() != null ? reg.getNumeroVersao() : 1) + 1);
        reg.setDataAtualizacao(LocalDateTime.now());

        ArgumentoRegistro salvo = argumentoRepository.save(reg);
        salvarSnapshotHistorico(salvo, "EDIÇÃO", editor);

        telemetriaService.registrarEvento(
                TipoEventoLog.EDICAO_ARGUMENTO,
                editor,
                reg.getGrupo(),
                salvo.getId(),
                "Argumento editado (v" + salvo.getNumeroVersao() + ")"
        );

        notificarAtualizacaoTempoReal(salvo, "EDICAO_ARGUMENTO");
        return salvo;
    }

    @Transactional
    public ArgumentoRegistro registrarContraponto(
            UUID argumentoId,
            String contrapontoTexto,
            String delegacaoContraponto,
            Usuario autorContraponto
    ) {
        if (contrapontoTexto == null || contrapontoTexto.isBlank()) {
            throw new IllegalArgumentException("O texto do contraponto é obrigatório.");
        }

        ArgumentoRegistro reg = argumentoRepository.findById(argumentoId)
                .orElseThrow(() -> new IllegalArgumentException("Argumento não encontrado."));

        // Se o argumento não é compartilhado no plenário, apenas membros do grupo ou organização podem adicionar contraponto nele
        if (!Boolean.TRUE.equals(reg.getCompartilhadoPlenario()) && reg.getGrupo() != null) {
            if (!grupoService.temPermissaoEdicao(autorContraponto, reg.getGrupo())) {
                throw new SecurityException("Este argumento é privado da delegação e não aceita contrapontos externos.");
            }
        }

        String delegacaoResp = delegacaoContraponto;
        if (delegacaoResp == null || delegacaoResp.isBlank()) {
            List<Grupo> gruposAutor = grupoService.listarGruposVisiveisParaUsuario(autorContraponto);
            delegacaoResp = !gruposAutor.isEmpty() ? gruposAutor.get(0).getPaisDelegacao() : "Plenário";
        }

        reg.setContraponto(contrapontoTexto.trim());
        reg.setAutorContrapontoNome(autorContraponto.getNome());
        reg.setDelegacaoContraponto(delegacaoResp);
        reg.setUltimoEditor(autorContraponto);
        reg.setNumeroVersao((reg.getNumeroVersao() != null ? reg.getNumeroVersao() : 1) + 1);
        reg.setDataAtualizacao(LocalDateTime.now());

        ArgumentoRegistro salvo = argumentoRepository.save(reg);
        salvarSnapshotHistorico(salvo, "CONTRAPONTO", autorContraponto);

        telemetriaService.registrarEvento(
                TipoEventoLog.REGISTRO_CONTRAPONTO,
                autorContraponto,
                reg.getGrupo(),
                salvo.getId(),
                "Contraponto registrado por " + autorContraponto.getNome() + " (" + delegacaoResp + ")"
        );

        notificarAtualizacaoTempoReal(salvo, "NOVO_CONTRAPONTO");
        return salvo;
    }

    @Transactional
    public ArgumentoRegistro restaurarVersao(UUID historicoId, Usuario solicitante) {
        HistoricoArgumento hist = historicoRepository.findById(historicoId)
                .orElseThrow(() -> new IllegalArgumentException("Versão do histórico não encontrada."));

        ArgumentoRegistro reg = argumentoRepository.findById(hist.getArgumentoId())
                .orElseThrow(() -> new IllegalArgumentException("Argumento original não encontrado."));

        if (reg.getGrupo() != null && !grupoService.temPermissaoEdicao(solicitante, reg.getGrupo())) {
            throw new SecurityException("Sem permissão para restaurar versões deste grupo.");
        }

        reg.setDelegacao(hist.getDelegacao());
        reg.setArgumento(hist.getArgumento());
        reg.setJustificativa(hist.getJustificativa());
        reg.setFonte(hist.getFonte());
        reg.setLinkFonte(hist.getLinkFonte());
        reg.setContraponto(hist.getContraponto());
        reg.setUltimoEditor(solicitante);
        reg.setNumeroVersao((reg.getNumeroVersao() != null ? reg.getNumeroVersao() : 1) + 1);
        reg.setDataAtualizacao(LocalDateTime.now());

        ArgumentoRegistro salvo = argumentoRepository.save(reg);
        salvarSnapshotHistorico(salvo, "RESTAURAÇÃO (da v" + hist.getNumeroVersao() + ")", solicitante);

        telemetriaService.registrarEvento(
                TipoEventoLog.RESTAURACAO_VERSAO,
                solicitante,
                reg.getGrupo(),
                salvo.getId(),
                "Versão " + hist.getNumeroVersao() + " restaurada para v" + salvo.getNumeroVersao()
        );

        notificarAtualizacaoTempoReal(salvo, "RESTAURACAO_VERSAO");
        return salvo;
    }

    private void salvarSnapshotHistorico(ArgumentoRegistro reg, String tipoAlteracao, Usuario autor) {
        HistoricoArgumento hist = HistoricoArgumento.builder()
                .argumentoId(reg.getId())
                .grupoId(reg.getGrupo() != null ? reg.getGrupo().getId() : null)
                .numeroVersao(reg.getNumeroVersao())
                .tipoAlteracao(tipoAlteracao)
                .delegacao(reg.getDelegacao())
                .argumento(reg.getArgumento())
                .justificativa(reg.getJustificativa())
                .fonte(reg.getFonte())
                .linkFonte(reg.getLinkFonte())
                .contraponto(reg.getContraponto())
                .autorAlteracao(autor)
                .dataAlteracao(LocalDateTime.now())
                .build();
        historicoRepository.save(hist);
    }

    private void notificarAtualizacaoTempoReal(ArgumentoRegistro reg, String evento) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", evento);
            payload.put("id", reg.getId());
            payload.put("delegacao", reg.getDelegacao());
            payload.put("argumento", reg.getArgumento());
            payload.put("justificativa", reg.getJustificativa());
            payload.put("fonte", reg.getFonte());
            payload.put("linkFonte", reg.getLinkFonte());
            payload.put("contraponto", reg.getContraponto());
            payload.put("versao", reg.getNumeroVersao());
            payload.put("autor", reg.getUltimoEditor() != null ? reg.getUltimoEditor().getNome() : "Participante");

            if (reg.getGrupo() != null) {
                messagingTemplate.convertAndSend("/topic/grupo/" + reg.getGrupo().getId(), payload);
            }
            if (Boolean.TRUE.equals(reg.getCompartilhadoPlenario())) {
                messagingTemplate.convertAndSend("/topic/plenario", payload);
            }
        } catch (Exception e) {
            log.debug("Notificação WebSocket ignorada: {}", e.getMessage());
        }
    }

    private String normalizarLink(String link) {
        if (link == null || link.isBlank()) return "";
        String limpo = link.trim();
        if (!limpo.startsWith("http://") && !limpo.startsWith("https://")) {
            return "https://" + limpo;
        }
        return limpo;
    }
}
