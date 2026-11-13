package com.mov.security;

import com.mov.service.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CaptchaAuthenticationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login-process".equals(request.getServletPath())) {
            String captchaResponse = request.getParameter("cf-turnstile-response");
            String clientIp = request.getRemoteAddr();

            if (!captchaService.isCaptchaValid(captchaResponse, clientIp)) {
                response.sendRedirect("/login?error=true&captcha=false");
                return; // Bloqueia a requisição de chegar no UsernamePasswordAuthenticationFilter
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
