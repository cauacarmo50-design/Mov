package com.mov.controller;

import com.mov.model.Documento;
import com.mov.model.enums.TipoDocumento;
import com.mov.security.CustomUserDetails;
import com.mov.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @PostMapping("/documentos/upload")
    public String enviarDocumento(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("titulo") String titulo,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "tipoDocumento", defaultValue = "PESQUISA") TipoDocumento tipoDocumento,
            @RequestParam(value = "grupoId", required = false) UUID grupoId,
            @RequestParam(value = "publicoGeral", defaultValue = "false") boolean publicoGeral,
            @RequestParam(value = "origem", defaultValue = "/estudos") String origem,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            documentoService.enviarDocumento(
                    arquivo,
                    titulo,
                    descricao,
                    tipoDocumento,
                    grupoId,
                    publicoGeral,
                    userDetails.getUsuario()
            );
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Documento \"" + titulo + "\" enviado e disponibilizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Falha ao enviar documento: " + e.getMessage());
        }

        if (grupoId != null && ("/estudos".equals(origem) || origem.isBlank())) {
            return "redirect:/grupos/" + grupoId;
        }
        return "redirect:" + (origem.startsWith("/") ? origem : "/estudos");
    }

    @GetMapping("/documentos/{id}/visualizar")
    public ResponseEntity<Resource> visualizarDocumento(
            @PathVariable("id") UUID docId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Documento doc = documentoService.obterDocumentoParaAcesso(docId, userDetails.getUsuario(), false);
        Resource resource = documentoService.carregarRecursoArquivo(doc);
        String contentType = doc.getContentType() != null ? doc.getContentType() : "application/pdf";
        String encodedFilename = URLEncoder.encode(doc.getNomeOriginal(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/documentos/{id}/download")
    public ResponseEntity<Resource> baixarDocumento(
            @PathVariable("id") UUID docId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Documento doc = documentoService.obterDocumentoParaAcesso(docId, userDetails.getUsuario(), true);
        Resource resource = documentoService.carregarRecursoArquivo(doc);
        String contentType = doc.getContentType() != null ? doc.getContentType() : "application/octet-stream";
        String encodedFilename = URLEncoder.encode(doc.getNomeOriginal(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PostMapping("/documentos/{id}/excluir")
    public String excluirDocumento(
            @PathVariable("id") UUID docId,
            @RequestParam(value = "origem", defaultValue = "/estudos") String origem,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            documentoService.excluirDocumento(docId, userDetails.getUsuario());
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Documento removido com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:" + (origem.startsWith("/") ? origem : "/estudos");
    }
}
