package com.mov.config;

import com.mov.model.enums.TipoEventoLog;
import com.mov.service.TelemetriaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final TelemetriaService telemetriaService;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        telemetriaService.registrarEvento(TipoEventoLog.ERRO_TECNICO, null, null, null, "Arquivo excedeu limite máximo permitido (20MB).");
        redirectAttributes.addFlashAttribute("mensagemErro", "O arquivo selecionado excede o tamanho máximo permitido (20 MB).");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && referer.contains("/grupos/") ? referer.substring(referer.indexOf("/grupos/")) : "/estudos");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLocking(ObjectOptimisticLockingFailureException exc, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("Conflito de edição simultânea detectado e protegido: {}", exc.getMessage());
        telemetriaService.registrarEvento(TipoEventoLog.ERRO_TECNICO, null, null, null, "Prevenção de sobrescrita acionada em edição simultânea.");
        redirectAttributes.addFlashAttribute("mensagemErro", "Outro integrante salvou uma alteração neste mesmo registro simultaneamente. A página foi atualizada para evitar sobrescrita de dados.");
        return "redirect:/dashboard";
    }

    @ExceptionHandler(SecurityException.class)
    public String handleSecurityException(SecurityException exc, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensagemErro", exc.getMessage());
        return "redirect:/dashboard";
    }
}
