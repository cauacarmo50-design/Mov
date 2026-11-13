package com.mov.controller;

import com.mov.model.Grupo;
import com.mov.model.Usuario;
import com.mov.model.enums.MomentoSimulacao;
import com.mov.model.enums.TipoDocumento;
import com.mov.model.enums.TipoUsuario;
import com.mov.security.CustomUserDetails;
import com.mov.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class AdminPilotoController {

    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final DocumentoService documentoService;
    private final ArgumentoService argumentoService;
    private final SalaDebateService salaDebateService;
    private final TelemetriaService telemetriaService;
    private final RelatorioExportService relatorioExportService;

    @GetMapping("/admin/piloto")
    public String painelPiloto(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Usuario admin = userDetails.getUsuario();
        List<Grupo> grupos = grupoService.listarTodosAtivos();
        List<Usuario> usuarios = usuarioService.listarTodos();

        Map<UUID, UUID> grupoAtualPorUsuario = new HashMap<>();
        Map<UUID, String> nomeGrupoPorUsuario = new HashMap<>();
        for (Grupo g : grupos) {
            if (g.getIntegrantes() != null) {
                for (Usuario m : g.getIntegrantes()) {
                    grupoAtualPorUsuario.put(m.getId(), g.getId());
                    nomeGrupoPorUsuario.put(m.getId(), g.getNome());
                }
            }
        }

        model.addAttribute("usuario", admin);
        model.addAttribute("indicadores", telemetriaService.obterIndicadoresPiloto());
        model.addAttribute("grupos", grupos);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("grupoAtualPorUsuario", grupoAtualPorUsuario);
        model.addAttribute("nomeGrupoPorUsuario", nomeGrupoPorUsuario);
        model.addAttribute("documentos", documentoService.listarTodosParaAdmin());
        model.addAttribute("argumentos", argumentoService.listarTodosAdmin());
        model.addAttribute("problemas", telemetriaService.listarProblemasTecnicos());
        model.addAttribute("logs", telemetriaService.listarLogsRecentes());
        model.addAttribute("momentoAtual", telemetriaService.getMomentoAtual());
        model.addAttribute("momentos", MomentoSimulacao.values());
        model.addAttribute("perfis", TipoUsuario.values());
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        model.addAttribute("salas", salaDebateService.listarTodasAtivas());
        return "admin-piloto";
    }

    @PostMapping("/admin/grupos/criar")
    public String criarGrupo(
            @RequestParam("nome") String nome,
            @RequestParam("paisDelegacao") String paisDelegacao,
            @RequestParam(value = "sigla", required = false) String sigla,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "salaId", required = false) UUID salaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Grupo criado = grupoService.criarGrupo(nome, paisDelegacao, sigla, descricao, salaId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Grupo \"" + criado.getNome() + "\" criado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-grupos";
    }

    @PostMapping("/admin/grupos/adicionar-membro")
    public String adicionarMembro(
            @RequestParam("grupoId") UUID grupoId,
            @RequestParam("usuarioId") UUID usuarioId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            grupoService.adicionarIntegrante(grupoId, usuarioId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Participante vinculado ao grupo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-grupos";
    }

    @PostMapping("/admin/grupos/adicionar-multiplos")
    public String adicionarMultiplosMembros(
            @RequestParam("grupoId") UUID grupoId,
            @RequestParam(value = "usuarioIds", required = false) List<UUID> usuarioIds,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            grupoService.adicionarMultiplosIntegrantes(grupoId, usuarioIds, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", usuarioIds.size() + " participante(s) adicionado(s) ao grupo de uma só vez!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-grupos";
    }

    @PostMapping("/admin/grupos/remover-membro")
    public String removerMembro(
            @RequestParam("grupoId") UUID grupoId,
            @RequestParam("usuarioId") UUID usuarioId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            grupoService.removerIntegrante(grupoId, usuarioId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Participante removido do grupo.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-grupos";
    }

    @PostMapping("/admin/participantes/alocar-grupo")
    public String alocarGrupoParticipante(
            @RequestParam("usuarioId") UUID usuarioId,
            @RequestParam(value = "novoGrupoId", required = false) UUID novoGrupoId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            grupoService.definirGrupoDoParticipante(usuarioId, novoGrupoId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Delegação do participante atualizada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-participantes";
    }

    @PostMapping("/admin/participantes/criar")
    public String criarParticipante(
            @RequestParam("nome") String nome,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "senha", defaultValue = "Senac@2026") String senha,
            @RequestParam(value = "tipo", defaultValue = "DELEGADO") TipoUsuario tipo,
            @RequestParam(value = "grupoId", required = false) UUID grupoId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario salvo = usuarioService.criarOuAtualizarParticipanteOrganizacao(nome, email, senha, tipo, "Senac SP");
            if (grupoId != null) {
                grupoService.adicionarIntegrante(grupoId, salvo.getId(), userDetails.getUsuario());
            }
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Conta de \"" + salvo.getNome() + "\" criada com sucesso! O aluno já pode entrar digitando o Nome Completo dele e a senha " + senha);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-participantes";
    }

    @PostMapping("/admin/participantes/lote")
    public String importarParticipantesEmLote(
            @RequestParam(value = "linhasParticipantes", required = false) String linhasParticipantes,
            @RequestParam(value = "arquivoCsv", required = false) MultipartFile arquivoCsv,
            @RequestParam(value = "senhaPadrao", defaultValue = "Senac@2026") String senhaPadrao,
            @RequestParam(value = "grupoId", required = false) UUID grupoId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        int processados = 0;
        try {
            StringBuilder conteudoTotal = new StringBuilder();
            if (linhasParticipantes != null && !linhasParticipantes.isBlank()) {
                conteudoTotal.append(linhasParticipantes).append("\n");
            }
            if (arquivoCsv != null && !arquivoCsv.isEmpty()) {
                conteudoTotal.append(new String(arquivoCsv.getBytes(), StandardCharsets.UTF_8));
            }

            String[] linhas = conteudoTotal.toString().replace("\uFEFF", "").split("\\r?\\n");
            for (String linha : linhas) {
                if (linha == null || linha.isBlank()) continue;
                String linhaTrim = linha.trim();

                // Ignora cabeçalho de planilha se for "nome;delegacao" ou "nome;email"
                String linhaLower = linhaTrim.toLowerCase();
                if (linhaLower.equals("nome") || linhaLower.startsWith("nome;") || linhaLower.startsWith("delegacao;") || linhaLower.startsWith("delegação;") || linhaLower.startsWith("pais;") || linhaLower.startsWith("país;")) {
                    continue;
                }

                String[] partes = linhaTrim.split("[;,\\t]");

                if (partes.length == 1) {
                    // Formato 1 coluna: Apenas Nome Completo (sem e-mail!)
                    String nome = partes[0].trim();
                    if (!nome.isEmpty()) {
                        Usuario u = usuarioService.criarOuAtualizarParticipanteOrganizacao(nome, null, senhaPadrao, TipoUsuario.DELEGADO, "Senac SP");
                        if (grupoId != null) {
                            grupoService.adicionarIntegrante(grupoId, u.getId(), userDetails.getUsuario());
                        }
                        processados++;
                    }
                } else if (partes.length == 2) {
                    String p0 = partes[0].trim();
                    String p1 = partes[1].trim();
                    if (p1.contains("@")) {
                        // Nome ; E-mail
                        Usuario u = usuarioService.criarOuAtualizarParticipanteOrganizacao(p0, p1, senhaPadrao, TipoUsuario.DELEGADO, "Senac SP");
                        if (grupoId != null) {
                            grupoService.adicionarIntegrante(grupoId, u.getId(), userDetails.getUsuario());
                        }
                        processados++;
                    } else if (!p0.isEmpty() && !p1.isEmpty()) {
                        // País/Delegação ; Nome Completo (sem e-mail!)
                        String paisOuDelegacao = p0;
                        String nome = p1;
                        Usuario u = usuarioService.criarOuAtualizarParticipanteOrganizacao(nome, null, senhaPadrao, TipoUsuario.DELEGADO, "Senac SP");
                        Grupo g = grupoService.buscarOuCriarGrupoPorPais(paisOuDelegacao, userDetails.getUsuario());
                        grupoService.adicionarIntegrante(g.getId(), u.getId(), userDetails.getUsuario());
                        processados++;
                    }
                } else if (partes.length >= 3) {
                    // Formato 3 colunas: Delegação/País ; Nome ; E-mail (ou sem e-mail)
                    String paisOuDelegacao = partes[0].trim();
                    String nome = partes[1].trim();
                    String email = partes[2].trim();
                    if (!nome.isEmpty()) {
                        Usuario u = usuarioService.criarOuAtualizarParticipanteOrganizacao(nome, email, senhaPadrao, TipoUsuario.DELEGADO, "Senac SP");
                        if (!paisOuDelegacao.isEmpty()) {
                            Grupo g = grupoService.buscarOuCriarGrupoPorPais(paisOuDelegacao, userDetails.getUsuario());
                            grupoService.adicionarIntegrante(g.getId(), u.getId(), userDetails.getUsuario());
                        } else if (grupoId != null) {
                            grupoService.adicionarIntegrante(grupoId, u.getId(), userDetails.getUsuario());
                        }
                        processados++;
                    }
                }
            }
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    processados + " participante(s) cadastrado(s) com sucesso! Eles já podem fazer login usando o próprio Nome Completo."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao importar planilha/lote: " + e.getMessage());
        }
        return "redirect:/admin/piloto#secao-participantes";
    }

    @PostMapping("/admin/participantes/redefinir-senha")
    public String redefinirSenha(
            @RequestParam("usuarioId") UUID usuarioId,
            @RequestParam("novaSenha") String novaSenha,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.redefinirSenha(usuarioId, novaSenha);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Senha redefinida e conta desbloqueada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-participantes";
    }

    @PostMapping("/admin/participantes/perfil")
    public String alterarPerfil(
            @RequestParam("usuarioId") UUID usuarioId,
            @RequestParam("novoTipo") TipoUsuario novoTipo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.alterarPerfil(usuarioId, novoTipo);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Perfil do usuário atualizado para " + novoTipo.name() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/admin/piloto#secao-participantes";
    }

    @PostMapping("/admin/momento")
    public String alterarMomentoSimulacao(
            @RequestParam("momento") MomentoSimulacao momento,
            RedirectAttributes redirectAttributes
    ) {
        telemetriaService.setMomentoAtual(momento);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Momento da simulação alterado para: " + momento.getDescricao());
        return "redirect:/admin/piloto";
    }

    @PostMapping("/admin/problemas/atualizar")
    public String atualizarProblema(
            @RequestParam("problemaId") UUID problemaId,
            @RequestParam("status") String status,
            @RequestParam(value = "respostaEquipe", required = false) String respostaEquipe,
            RedirectAttributes redirectAttributes
    ) {
        telemetriaService.atualizarStatusProblema(problemaId, status, respostaEquipe);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Status do chamado técnico atualizado.");
        return "redirect:/admin/piloto#secao-problemas";
    }

    @GetMapping("/admin/exportar/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(value = "anonimizado", defaultValue = "true") boolean anonimizado,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        byte[] conteudo = relatorioExportService.gerarCsvRelatorioPiloto(anonimizado, userDetails.getUsuario());
        String sufixo = anonimizado ? "anonimizado" : "detalhado";
        String filename = "relatorio_piloto_mov_senac_" + sufixo + "_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(conteudo);
    }

    @GetMapping("/admin/exportar/xlsx")
    public ResponseEntity<byte[]> exportarXlsx(
            @RequestParam(value = "anonimizado", defaultValue = "true") boolean anonimizado,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        byte[] conteudo = relatorioExportService.gerarXlsxRelatorioPiloto(anonimizado, userDetails.getUsuario());
        String sufixo = anonimizado ? "anonimizado" : "detalhado";
        String filename = "relatorio_piloto_mov_senac_" + sufixo + "_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(conteudo);
    }

    @PostMapping("/suporte/reportar")
    public String reportarProblemaTecnico(
            @RequestParam("categoria") String categoria,
            @RequestParam("descricao") String descricao,
            @RequestParam(value = "grupoId", required = false) UUID grupoId,
            @RequestParam(value = "paginaOrigem", defaultValue = "/dashboard") String paginaOrigem,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        Usuario u = userDetails != null ? userDetails.getUsuario() : null;
        Grupo g = null;
        if (grupoId != null && u != null) {
            List<Grupo> visiveis = grupoService.listarGruposVisiveisParaUsuario(u);
            g = visiveis.stream().filter(gr -> gr.getId().equals(grupoId)).findFirst().orElse(null);
        }
        telemetriaService.reportarProblema(u, g, categoria, descricao, paginaOrigem);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Problema técnico registrado! A equipe de suporte do M.O.V. já recebeu seu alerta.");
        return "redirect:" + (paginaOrigem.startsWith("/") ? paginaOrigem : "/dashboard");
    }
}
