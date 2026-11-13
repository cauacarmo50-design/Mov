package com.mov.security;

import com.mov.model.DispositivoConhecido;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoEventoLog;
import com.mov.repository.DispositivoConhecidoRepository;
import com.mov.repository.UsuarioRepository;
import com.mov.service.EmailService;
import com.mov.service.TelemetriaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationSuccessEventListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final UsuarioRepository usuarioRepository;
    private final DispositivoConhecidoRepository dispositivoRepository;
    private final EmailService emailService;
    private final TelemetriaService telemetriaService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Override
    @Transactional
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication().getPrincipal() instanceof CustomUserDetails userDetails)) {
            return;
        }
        String email = userDetails.getUsername();

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            // 1. Resetar tentativas falhas (Lockout)
            if (usuario.getTentativasFalhasLogin() != null && usuario.getTentativasFalhasLogin() > 0) {
                usuario.setTentativasFalhasLogin(0);
                usuario.setContaBloqueadaAte(null);
                usuarioRepository.save(usuario);
            }

            // 2. Registrar indicador de Acesso (Seção 6)
            telemetriaService.registrarEvento(
                    TipoEventoLog.LOGIN,
                    usuario,
                    null,
                    usuario.getId(),
                    "Login realizado com sucesso por " + usuario.getEmail()
            );

            // 3. Device Fingerprinting
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpServletRequest request = attr.getRequest();
                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent == null) userAgent = "Desconhecido";

                String finalUserAgent = userAgent;
                boolean isNewDevice = dispositivoRepository
                        .findByUsuarioAndIpAddressAndUserAgent(usuario, ipAddress, userAgent)
                        .isEmpty();

                if (isNewDevice) {
                    DispositivoConhecido novoDispositivo = DispositivoConhecido.builder()
                            .usuario(usuario)
                            .ipAddress(ipAddress)
                            .userAgent(finalUserAgent)
                            .build();
                    dispositivoRepository.save(novoDispositivo);

                    if (mailEnabled) {
                        try {
                            emailService.enviarAlertaNovoDispositivo(usuario.getEmail(), usuario.getNome(), ipAddress, finalUserAgent);
                        } catch (Exception e) {
                            log.warn("Envio de alerta de dispositivo ignorado: {}", e.getMessage());
                        }
                    }
                } else {
                    dispositivoRepository.findByUsuarioAndIpAddressAndUserAgent(usuario, ipAddress, userAgent)
                            .ifPresent(disp -> {
                                disp.setDataUltimoAcesso(LocalDateTime.now());
                                dispositivoRepository.save(disp);
                            });
                }
            }
        });
    }
}
