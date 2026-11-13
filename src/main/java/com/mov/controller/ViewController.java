package com.mov.controller;

import com.mov.model.Grupo;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoDocumento;
import com.mov.security.CustomUserDetails;
import com.mov.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final SalaDebateService salaDebateService;
    private final GrupoService grupoService;
    private final DocumentoService documentoService;
    private final ArgumentoService argumentoService;
    private final TelemetriaService telemetriaService;

    @Value("${CAPTCHA_SITE_KEY:0x4AAAAAAD95MMShuTBDAW1g}")
    private String captchaSiteKey;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @GetMapping("/")
    public String root() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Usuario usuario = userDetails.getUsuario();
            model.addAttribute("usuario", usuario);
            model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
        }
        model.addAttribute("momentoAtual", telemetriaService.getMomentoAtual());
        return "inicio";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("siteKey", captchaSiteKey);
        model.addAttribute("captchaEnabled", captchaEnabled);
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("siteKey", captchaSiteKey);
        model.addAttribute("captchaEnabled", captchaEnabled);
        model.addAttribute("gruposDisponiveis", grupoService.listarTodosAtivos());
        return "cadastro";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Usuario usuario = userDetails.getUsuario();
            model.addAttribute("usuario", usuario);
            List<Grupo> meusGrupos = grupoService.listarGruposVisiveisParaUsuario(usuario);
            model.addAttribute("meusGrupos", meusGrupos);
            model.addAttribute("todosGruposDisponiveis", grupoService.listarTodosAtivos());
            model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
            model.addAttribute("documentosRecentes", documentoService.listarDocumentosAcessiveisPeloUsuario(usuario));
        }
        model.addAttribute("momentoAtual", telemetriaService.getMomentoAtual());
        model.addAttribute("salas", salaDebateService.listarTodasAtivas());
        return "dashboard";
    }

    @GetMapping("/consenso")
    public String consenso(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Usuario usuario = userDetails.getUsuario();
            model.addAttribute("usuario", usuario);
            model.addAttribute("meusGrupos", grupoService.listarGruposVisiveisParaUsuario(usuario));
            model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
        }
        model.addAttribute("argumentosPlenario", argumentoService.listarCompartilhadosPlenario());
        return "consenso";
    }

    @GetMapping("/estudos")
    public String estudos(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Usuario usuario = userDetails.getUsuario();
            model.addAttribute("usuario", usuario);
            model.addAttribute("meusGrupos", grupoService.listarGruposVisiveisParaUsuario(usuario));
            model.addAttribute("documentos", documentoService.listarDocumentosAcessiveisPeloUsuario(usuario));
            model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
        } else {
            model.addAttribute("documentos", documentoService.listarDocumentosGerais());
        }
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        return "estudos";
    }

    @GetMapping("/leitura")
    public String leitura(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Usuario usuario = userDetails.getUsuario();
            model.addAttribute("usuario", usuario);
            model.addAttribute("documentos", documentoService.listarDocumentosAcessiveisPeloUsuario(usuario));
        } else {
            model.addAttribute("documentos", documentoService.listarDocumentosGerais());
        }
        return "leitura";
    }

    @GetMapping("/quiz")
    public String quiz(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("usuario", userDetails.getUsuario());
        }
        return "quiz";
    }

    @GetMapping("/regrasdasala")
    public String regrasdasala(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("usuario", userDetails.getUsuario());
        }
        return "regrasdasala";
    }

    @GetMapping("/simulacao")
    public String simulacao(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Usuario usuario = userDetails.getUsuario();
            model.addAttribute("usuario", usuario);
            model.addAttribute("meusGrupos", grupoService.listarGruposVisiveisParaUsuario(usuario));
            model.addAttribute("isOrganizacao", grupoService.isEquipeOrganizacao(usuario));
        }
        model.addAttribute("momentoAtual", telemetriaService.getMomentoAtual());
        model.addAttribute("argumentosPlenario", argumentoService.listarCompartilhadosPlenario());
        return "simulacao";
    }
}
