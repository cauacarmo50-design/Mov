package com.mov.controller;

import com.mov.model.Grupo;
import com.mov.model.Usuario;
import com.mov.security.CustomUserDetails;
import com.mov.service.GrupoService;
import com.mov.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;
    private final GrupoService grupoService;

    @GetMapping
    public String exibirPerfil(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioService.buscarPorId(userDetails.getUsuario().getId())
                .orElse(userDetails.getUsuario());
        List<Grupo> meusGrupos = grupoService.listarGruposVisiveisParaUsuario(usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("meusGrupos", meusGrupos);
        model.addAttribute("todosGruposDisponiveis", grupoService.listarTodosAtivos());
        model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
        return "perfil";
    }

    @PostMapping("/dados")
    public String atualizarDados(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String nome,
            @RequestParam(required = false) String instituicao,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            Usuario atualizado = usuarioService.atualizarDadosProprios(
                    userDetails.getUsuario().getId(),
                    nome,
                    instituicao
            );
            userDetails.getUsuario().setNome(atualizado.getNome());
            userDetails.getUsuario().setInstituicao(atualizado.getInstituicao());
            redirectAttributes.addFlashAttribute("sucesso", "Seus dados de perfil foram atualizados com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/perfil";
    }

    @PostMapping("/senha")
    public String alterarSenha(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha,
            @RequestParam String confirmarSenha,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            usuarioService.alterarSenhaPropria(
                    userDetails.getUsuario().getId(),
                    senhaAtual,
                    novaSenha,
                    confirmarSenha
            );
            redirectAttributes.addFlashAttribute("sucesso", "Sua senha foi alterada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/perfil";
    }
}
