package com.mov.controller;

import com.mov.dto.UsuarioCadastroRecord;
import com.mov.model.Grupo;
import com.mov.model.Usuario;
import com.mov.repository.GrupoRepository;
import com.mov.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final GrupoRepository grupoRepository;

    @PostMapping("/cadastrar")
    public String cadastrar(
            @Valid UsuarioCadastroRecord dto,
            BindingResult result,
            @RequestParam(value = "grupoId", required = false) UUID grupoId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            var errorMessage = result.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("mensagemErro", errorMessage);
            return "redirect:/cadastro";
        }

        try {
            var clientIp = request.getRemoteAddr();
            Usuario novoUsuario = usuarioService.registrarUsuario(dto, clientIp);

            if (grupoId != null) {
                grupoRepository.findById(grupoId).ifPresent(Grupo -> {
                    Grupo.getIntegrantes().add(novoUsuario);
                    grupoRepository.save(Grupo);
                });
            }

            redirectAttributes.addFlashAttribute("mensagemSucesso", "Conta criada e vinculada com sucesso! Você já pode fazer login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
            return "redirect:/cadastro";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao realizar cadastro. Tente novamente.");
            return "redirect:/cadastro";
        }
    }

    @GetMapping("/confirmar")
    public String confirmarConta(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        var confirmado = usuarioService.confirmarConta(token);
        if (confirmado) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Conta ativada com sucesso! Você já pode fazer login.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Token de ativação inválido ou expirado.");
        }
        return "redirect:/login";
    }
}
