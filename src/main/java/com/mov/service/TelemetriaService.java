package com.mov.service;

import com.mov.model.*;
import com.mov.model.enums.MomentoSimulacao;
import com.mov.model.enums.TipoEventoLog;
import com.mov.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetriaService {

    private final LogAtividadeRepository logAtividadeRepository;
    private final ProblemaTecnicoRepository problemaTecnicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GrupoRepository grupoRepository;
    private final DocumentoRepository documentoRepository;
    private final ArgumentoRegistroRepository argumentoRepository;
    private final HistoricoArgumentoRepository historicoArgumentoRepository;

    private volatile MomentoSimulacao momentoAtual = MomentoSimulacao.ANTES_EVENTO;

    public MomentoSimulacao getMomentoAtual() {
        return momentoAtual;
    }

    public void setMomentoAtual(MomentoSimulacao novoMomento) {
        if (novoMomento != null) {
            this.momentoAtual = novoMomento;
        }
    }

    @Transactional
    public void registrarEvento(TipoEventoLog tipo, Usuario usuario, Grupo grupo, UUID recursoId, String detalhes) {
        try {
            String dispositivo = detectarDispositivoAtual();
            LogAtividade registro = LogAtividade.builder()
                    .tipoEvento(tipo)
                    .usuarioId(usuario != null ? usuario.getId() : null)
                    .usuarioNome(usuario != null ? usuario.getNome() : "Anônimo/Sistema")
                    .usuarioPerfil(usuario != null && usuario.getTipo() != null ? usuario.getTipo().name() : "N/A")
                    .grupoId(grupo != null ? grupo.getId() : null)
                    .grupoNome(grupo != null ? grupo.getNome() : null)
                    .recursoId(recursoId)
                    .detalhes(detalhes)
                    .momento(momentoAtual)
                    .dispositivo(dispositivo)
                    .dataHora(LocalDateTime.now())
                    .build();
            logAtividadeRepository.save(registro);
        } catch (Exception e) {
            log.warn("Falha não-bloqueante ao gravar telemetria: {}", e.getMessage());
        }
    }

    @Transactional
    public ProblemaTecnico reportarProblema(Usuario usuario, Grupo grupo, String categoria, String descricao, String paginaOrigem) {
        String disp = detectarDispositivoAtual();
        ProblemaTecnico problema = ProblemaTecnico.builder()
                .usuario(usuario)
                .grupo(grupo)
                .categoria(categoria != null && !categoria.isBlank() ? categoria : "Geral")
                .descricao(descricao)
                .paginaOrigem(paginaOrigem)
                .dispositivoInfo(disp)
                .status("ABERTO")
                .dataRegistro(LocalDateTime.now())
                .build();
        ProblemaTecnico salvo = problemaTecnicoRepository.save(problema);
        registrarEvento(TipoEventoLog.PROBLEMA_REPORTADO, usuario, grupo, salvo.getId(), "Problema reportado: " + categoria);
        return salvo;
    }

    @Transactional
    public void atualizarStatusProblema(UUID problemaId, String novoStatus, String respostaEquipe) {
        problemaTecnicoRepository.findById(problemaId).ifPresent(p -> {
            p.setStatus(novoStatus);
            p.setRespostaEquipe(respostaEquipe);
            if ("RESOLVIDO".equalsIgnoreCase(novoStatus)) {
                p.setDataResolucao(LocalDateTime.now());
            }
            problemaTecnicoRepository.save(p);
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obterIndicadoresPiloto() {
        Map<String, Object> ind = new LinkedHashMap<>();

        long totalUsuariosCadastrados = usuarioRepository.count();
        long usuariosComAcesso = logAtividadeRepository.countUsuariosUnicosComAcesso();
        long totalLogins = logAtividadeRepository.countByTipoEvento(TipoEventoLog.LOGIN);

        List<TipoEventoLog> eventosContribuicao = List.of(
                TipoEventoLog.REGISTRO_ARGUMENTO,
                TipoEventoLog.EDICAO_ARGUMENTO,
                TipoEventoLog.REGISTRO_CONTRAPONTO,
                TipoEventoLog.UPLOAD_DOCUMENTO,
                TipoEventoLog.EDICAO_TEXTO_GRUPO
        );
        long usuariosComContribuicao = logAtividadeRepository.countUsuariosComContribuicao(eventosContribuicao);

        List<Grupo> grupos = grupoRepository.findByAtivoTrueOrderByNomeAsc();
        List<Documento> documentos = documentoRepository.findAllByOrderByDataEnvioDesc();
        long totalConsultasDocumentos = documentos.stream()
                .mapToLong(d -> d.getTotalConsultas() != null ? d.getTotalConsultas() : 0)
                .sum();

        long totalArgumentos = argumentoRepository.count();
        long totalFontes = argumentoRepository.countComFonte();
        long totalContrapontos = argumentoRepository.countComContraponto();
        long totalAlteracoesHistorico = historicoArgumentoRepository.count();
        long totalProblemas = problemaTecnicoRepository.count();
        long problemasAbertos = problemaTecnicoRepository.countByStatus("ABERTO");
        long acessosIndevidosBloqueados = logAtividadeRepository.countByTipoEvento(TipoEventoLog.ACESSO_INDEVIDO_BLOQUEADO);

        ind.put("totalUsuariosCadastrados", totalUsuariosCadastrados);
        ind.put("usuariosComAcesso", Math.max(usuariosComAcesso, 1));
        ind.put("totalLogins", totalLogins);
        ind.put("usuariosComContribuicao", usuariosComContribuicao);
        ind.put("totalGrupos", grupos.size());
        ind.put("totalDocumentos", documentos.size());
        ind.put("totalConsultasDocumentos", totalConsultasDocumentos);
        ind.put("totalArgumentos", totalArgumentos);
        ind.put("totalFontes", totalFontes);
        ind.put("totalContrapontos", totalContrapontos);
        ind.put("totalAlteracoesHistorico", totalAlteracoesHistorico);
        ind.put("totalProblemas", totalProblemas);
        ind.put("problemasAbertos", problemasAbertos);
        ind.put("acessosIndevidosBloqueados", acessosIndevidosBloqueados);
        ind.put("momentoAtual", momentoAtual);

        // Estatísticas detalhadas por grupo (Seção 6.1)
        List<Map<String, Object>> metricasPorGrupo = new ArrayList<>();
        for (Grupo g : grupos) {
            Map<String, Object> mg = new LinkedHashMap<>();
            List<ArgumentoRegistro> argsGrupo = argumentoRepository.findByGrupoIdOrderByDataAtualizacaoDesc(g.getId());
            long fontesGrupo = argsGrupo.stream().filter(a -> a.getFonte() != null && !a.getFonte().isBlank()).count();
            long contrapontosGrupo = argsGrupo.stream().filter(a -> a.getContraponto() != null && !a.getContraponto().isBlank()).count();
            long docsGrupo = documentoRepository.countByGrupoId(g.getId());
            long ativosGrupo = logAtividadeRepository.countUsuariosAtivosNoGrupo(g.getId());

            mg.put("id", g.getId());
            mg.put("nome", g.getNome());
            mg.put("paisDelegacao", g.getPaisDelegacao());
            mg.put("totalIntegrantes", g.getIntegrantes() != null ? g.getIntegrantes().size() : 0);
            mg.put("usuariosAtivos", ativosGrupo);
            mg.put("totalDocumentos", docsGrupo);
            mg.put("totalArgumentos", argsGrupo.size());
            mg.put("totalFontes", fontesGrupo);
            mg.put("totalContrapontos", contrapontosGrupo);
            metricasPorGrupo.add(mg);
        }
        ind.put("metricasPorGrupo", metricasPorGrupo);

        return ind;
    }

    public List<LogAtividade> listarLogsRecentes() {
        return logAtividadeRepository.findTop200ByOrderByDataHoraDesc();
    }

    public List<ProblemaTecnico> listarProblemasTecnicos() {
        return problemaTecnicoRepository.findAllByOrderByDataRegistroDesc();
    }

    public static String anonimizarIdentificador(UUID id) {
        if (id == null) return "Participante #ANON";
        String hex = Integer.toHexString(Math.abs(id.hashCode())).toUpperCase();
        if (hex.length() > 5) hex = hex.substring(0, 5);
        return "Participante #" + hex;
    }

    private String detectarDispositivoAtual() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpServletRequest request = attr.getRequest();
                String ua = request.getHeader("User-Agent");
                if (ua != null) {
                    String lower = ua.toLowerCase();
                    if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone") || lower.contains("ipad")) {
                        return "Celular / Dispositivo Móvel";
                    }
                    return "Computador / Navegador Desktop";
                }
            }
        } catch (Exception ignored) {
        }
        return "Navegador Web";
    }
}
