package com.mov.controller;

import com.mov.model.ArgumentoRegistro;
import com.mov.model.Grupo;
import com.mov.model.HistoricoArgumento;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoDocumento;
import com.mov.model.enums.TipoEventoLog;
import com.mov.security.CustomUserDetails;
import com.mov.service.ArgumentoService;
import com.mov.service.DocumentoService;
import com.mov.service.GrupoService;
import com.mov.service.TelemetriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;
    private final DocumentoService documentoService;
    private final ArgumentoService argumentoService;
    private final TelemetriaService telemetriaService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @PostMapping("/grupos/entrar")
    public String entrarNaDelegacaoAutoAtendimento(
            @RequestParam("grupoId") UUID grupoId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Grupo grupo = grupoService.entrarNoGrupoAutoAtendimento(grupoId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Você entrou em " + grupo.getNome() + " com sucesso!");
            return "redirect:/grupos/" + grupo.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/grupos/{id}")
    public String exibirGrupo(
            @PathVariable("id") UUID grupoId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario usuario = userDetails.getUsuario();
            Grupo grupo = grupoService.buscarPorIdComPermissao(grupoId, usuario);

            telemetriaService.registrarEvento(
                    TipoEventoLog.ACESSO_GRUPO,
                    usuario,
                    grupo,
                    grupo.getId(),
                    "Acesso ao espaço do " + grupo.getNome()
            );

            model.addAttribute("usuario", usuario);
            model.addAttribute("grupo", grupo);
            model.addAttribute("podeEditar", grupoService.temPermissaoEdicao(usuario, grupo));
            model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
            model.addAttribute("momentoAtual", telemetriaService.getMomentoAtual());
            model.addAttribute("argumentos", argumentoService.listarPorGrupo(grupoId, usuario));
            model.addAttribute("historicoGrupo", argumentoService.listarHistoricoDoGrupo(grupoId, usuario));
            model.addAttribute("documentosGrupo", documentoService.listarDocumentosDoGrupo(grupoId, usuario));
            model.addAttribute("documentosGerais", documentoService.listarDocumentosGerais());
            model.addAttribute("tiposDocumento", TipoDocumento.values());

            return "grupo-detalhe";
        } catch (SecurityException se) {
            redirectAttributes.addFlashAttribute("mensagemErro", se.getMessage());
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não foi possível abrir o grupo solicitado: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/grupos/{id}/argumentos")
    public String adicionarArgumentoNoGrupo(
            @PathVariable("id") UUID grupoId,
            @RequestParam(value = "delegacao", required = false) String delegacao,
            @RequestParam("argumento") String argumento,
            @RequestParam(value = "justificativa", required = false) String justificativa,
            @RequestParam(value = "fonte", required = false) String fonte,
            @RequestParam(value = "linkFonte", required = false) String linkFonte,
            @RequestParam(value = "contraponto", required = false) String contraponto,
            @RequestParam(value = "compartilhadoPlenario", defaultValue = "false") boolean compartilhadoPlenario,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            argumentoService.registrarArgumento(
                    grupoId,
                    delegacao,
                    argumento,
                    justificativa,
                    fonte,
                    linkFonte,
                    contraponto,
                    compartilhadoPlenario,
                    userDetails.getUsuario()
            );
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Argumento e referências salvos com sucesso no arquivo compartilhado!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/grupos/" + grupoId;
    }

    @PostMapping("/grupos/{id}/argumentos/{argId}/editar")
    public String editarArgumentoNoGrupo(
            @PathVariable("id") UUID grupoId,
            @PathVariable("argId") UUID argId,
            @RequestParam(value = "delegacao", required = false) String delegacao,
            @RequestParam("argumento") String argumento,
            @RequestParam(value = "justificativa", required = false) String justificativa,
            @RequestParam(value = "fonte", required = false) String fonte,
            @RequestParam(value = "linkFonte", required = false) String linkFonte,
            @RequestParam(value = "contraponto", required = false) String contraponto,
            @RequestParam(value = "compartilhadoPlenario", defaultValue = "false") boolean compartilhadoPlenario,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ArgumentoRegistro atualizado = argumentoService.editarArgumento(
                    argId,
                    delegacao,
                    argumento,
                    justificativa,
                    fonte,
                    linkFonte,
                    contraponto,
                    compartilhadoPlenario,
                    userDetails.getUsuario()
            );
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Registro atualizado com sucesso (Versão v" + atualizado.getNumeroVersao() + " salva no histórico).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/grupos/" + grupoId;
    }

    @PostMapping("/grupos/{id}/argumentos/{argId}/contraponto")
    public String registrarContrapontoNoGrupo(
            @PathVariable("id") UUID grupoId,
            @PathVariable("argId") UUID argId,
            @RequestParam("contraponto") String contraponto,
            @RequestParam(value = "delegacaoContraponto", required = false) String delegacaoContraponto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            argumentoService.registrarContraponto(argId, contraponto, delegacaoContraponto, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Contraponto registrado e salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/grupos/" + grupoId;
    }

    @PostMapping("/grupos/{id}/historico/{histId}/restaurar")
    public String restaurarVersaoNoGrupo(
            @PathVariable("id") UUID grupoId,
            @PathVariable("histId") UUID histId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ArgumentoRegistro restaurado = argumentoService.restaurarVersao(histId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Versão anterior restaurada com sucesso (nova versão v" + restaurado.getNumeroVersao() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/grupos/" + grupoId;
    }

    @PostMapping("/grupos/{id}/texto-colaborativo")
    public String salvarTextoColaborativo(
            @PathVariable("id") UUID grupoId,
            @RequestParam("textoColaborativo") String textoColaborativo,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            grupoService.atualizarTextoColaborativo(grupoId, textoColaborativo, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Documento de posição colaborativo salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/grupos/" + grupoId;
    }

    @PostMapping("/api/grupos/{id}/texto-colaborativo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> autoSalvarTextoColaborativoJson(
            @PathVariable("id") UUID grupoId,
            @RequestParam("textoColaborativo") String textoColaborativo,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            Grupo salvo = grupoService.atualizarTextoColaborativo(grupoId, textoColaborativo, userDetails.getUsuario());
            resp.put("sucesso", true);
            resp.put("ultimoEditor", salvo.getUltimoEditorTextoNome());
            resp.put("horario", salvo.getDataAtualizacaoTexto() != null ? salvo.getDataAtualizacaoTexto().format(FMT) : "");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("sucesso", false);
            resp.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/argumentos/plenario")
    public String registrarArgumentoPlenario(
            @RequestParam(value = "grupoId", required = false) UUID grupoId,
            @RequestParam(value = "delegacao", required = false) String delegacao,
            @RequestParam("argumento") String argumento,
            @RequestParam(value = "justificativa", required = false) String justificativa,
            @RequestParam(value = "fonte", required = false) String fonte,
            @RequestParam(value = "linkFonte", required = false) String linkFonte,
            @RequestParam(value = "contraponto", required = false) String contraponto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            argumentoService.registrarArgumento(
                    grupoId,
                    delegacao,
                    argumento,
                    justificativa,
                    fonte,
                    linkFonte,
                    contraponto,
                    true,
                    userDetails.getUsuario()
            );
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Argumento registrado e compartilhado no plenário!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/simulacao";
    }

    @PostMapping("/argumentos/{argId}/contraponto-plenario")
    public String registrarContrapontoPlenario(
            @PathVariable("argId") UUID argId,
            @RequestParam("contraponto") String contraponto,
            @RequestParam(value = "delegacaoContraponto", required = false) String delegacaoContraponto,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            argumentoService.registrarContraponto(argId, contraponto, delegacaoContraponto, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Contraponto adicionado ao registro do plenário!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/simulacao";
    }

    @GetMapping("/api/grupos/{id}/argumentos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarArgumentosJson(
            @PathVariable("id") UUID grupoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ArgumentoRegistro> lista = argumentoService.listarPorGrupo(grupoId, userDetails.getUsuario());
        List<Map<String, Object>> resposta = new ArrayList<>();
        for (ArgumentoRegistro a : lista) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("delegacao", a.getDelegacao());
            item.put("argumento", a.getArgumento());
            item.put("justificativa", a.getJustificativa());
            item.put("fonte", a.getFonte());
            item.put("linkFonte", a.getLinkFonte());
            item.put("contraponto", a.getContraponto());
            item.put("autor", a.getAutor() != null ? a.getAutor().getNome() : "Participante");
            item.put("ultimoEditor", a.getUltimoEditor() != null ? a.getUltimoEditor().getNome() : "");
            item.put("versao", a.getNumeroVersao());
            item.put("dataAtualizacao", a.getDataAtualizacao() != null ? a.getDataAtualizacao().format(FMT) : "");
            resposta.add(item);
        }
        return ResponseEntity.ok(resposta);
    }
}
