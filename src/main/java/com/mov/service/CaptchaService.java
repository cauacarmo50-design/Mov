package com.mov.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class CaptchaService {

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${app.captcha.secret:}")
    private String captchaSecret;

    @Value("${app.captcha.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isCaptchaValid(String captchaResponse, String clientIp) {
        if (!captchaEnabled || captchaSecret == null || captchaSecret.isBlank()) {
            return true;
        }

        if (captchaResponse == null || captchaResponse.isEmpty()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", captchaSecret);
            map.add("response", captchaResponse);
            if (clientIp != null) {
                map.add("remoteip", clientIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null) {
                Boolean success = (Boolean) body.get("success");
                if (Boolean.TRUE.equals(success)) {
                    return true;
                } else {
                    log.warn("Falha no CAPTCHA: {}", body.get("error-codes"));
                }
            }
        } catch (Exception e) {
            log.error("Erro ao validar CAPTCHA na API externa (liberando em modo contingência): {}", e.getMessage());
            return true;
        }
        return false;
    }
}
