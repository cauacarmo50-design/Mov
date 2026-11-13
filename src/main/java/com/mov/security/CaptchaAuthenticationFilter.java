package com.mov.security;

import com.mov.model.Usuario;
import com.mov.repository.UsuarioRepository;
import com.mov.service.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CaptchaAuthenticationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login-process".equals(request.getServletPath())) {
            String captchaResponse = request.getParameter("cf-turnstile-response");
            String clientIp = request.getRemoteAddr();

            if (!captchaService.isCaptchaValid(captchaResponse, clientIp)) {
                response.sendRedirect("/login?error=true&captcha=false");
                return;
            }
        }

        // Atualiza em tempo real o perfil (ADMIN, PROFESSOR, etc.) na sessão caso a organização tenha alterado no banco
        String uri = request.getRequestURI();
        if (uri != null && !uri.startsWith("/css/") && !uri.startsWith("/js/") && !uri.startsWith("/img/")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                Usuario sessionUser = userDetails.getUsuario();
                if (sessionUser != null && sessionUser.getId() != null) {
                    usuarioRepository.findById(sessionUser.getId()).ifPresent(dbUser -> {
                        sessionUser.setNome(dbUser.getNome());
                        sessionUser.setInstituicao(dbUser.getInstituicao());
                        if (dbUser.getTipo() != sessionUser.getTipo()) {
                            sessionUser.setTipo(dbUser.getTipo());
                            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    auth.getCredentials(),
                                    userDetails.getAuthorities()
                            );
                            newAuth.setDetails(auth.getDetails());
                            SecurityContextHolder.getContext().setAuthentication(newAuth);
                        }
                    });
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
