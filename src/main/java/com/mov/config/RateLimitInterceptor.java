package com.mov.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();
    // Em simulação presencial (mesmo Wi-Fi/NAT do Senac), o limite por IP precisa comportar dezenas de alunos simultâneos
    private static final int MAX_REQUESTS_PER_USER = 600;
    private static final int MAX_REQUESTS_PER_IP = 3000;
    private static final long TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    @Value("${app.security.use-reverse-proxy:false}")
    private boolean useReverseProxy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/") || path.startsWith("/assets/"))) {
            return true;
        }

        Principal principal = request.getUserPrincipal();
        String rateKey;
        int limit;
        if (principal != null && principal.getName() != null) {
            rateKey = "USER:" + principal.getName();
            limit = MAX_REQUESTS_PER_USER;
        } else {
            rateKey = "IP:" + getClientIP(request);
            limit = MAX_REQUESTS_PER_IP;
        }

        long currentTime = System.currentTimeMillis();

        RequestInfo currentInfo = requestCounts.compute(rateKey, (key, requestInfo) -> {
            if (requestInfo == null || (currentTime - requestInfo.startTime > TIME_WINDOW_MS)) {
                return new RequestInfo(currentTime, 1);
            }
            requestInfo.count++;
            return requestInfo;
        });

        if (currentInfo.count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Muitas requisicoes simultaneas. Aguarde alguns segundos e tente novamente.");
            return false;
        }

        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        if (useReverseProxy) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null) {
                return xfHeader.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static class RequestInfo {
        long startTime;
        int count;

        RequestInfo(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
