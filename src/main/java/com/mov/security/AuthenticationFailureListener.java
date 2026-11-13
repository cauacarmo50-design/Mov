package com.mov.security;

import com.mov.model.Usuario;
import com.mov.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private final UsuarioRepository usuarioRepository;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 15;

    @Override
    @Transactional
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        String email = (String) event.getAuthentication().getPrincipal();

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            if (usuario.getContaBloqueadaAte() != null && usuario.getContaBloqueadaAte().isAfter(LocalDateTime.now())) {
                log.warn("Tentativa de login em conta bloqueada: {}", email);
                return;
            }

            int tentativas = usuario.getTentativasFalhasLogin() == null ? 0 : usuario.getTentativasFalhasLogin();
            tentativas++;

            if (tentativas >= MAX_FAILED_ATTEMPTS) {
                usuario.setContaBloqueadaAte(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
                usuario.setTentativasFalhasLogin(0);
                log.warn("Conta bloqueada por excesso de falhas: {}", email);
            } else {
                usuario.setTentativasFalhasLogin(tentativas);
                log.info("Falha de login número {} para o usuário: {}", tentativas, email);
            }
            
            usuarioRepository.save(usuario);
        });
    }
}
