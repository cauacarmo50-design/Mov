package com.mov.security;

import com.mov.model.Usuario;
import com.mov.repository.UsuarioRepository;
import com.mov.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        String limpo = identificador != null ? identificador.trim() : "";

        Optional<Usuario> opt = usuarioRepository.findByEmail(limpo.toLowerCase());
        if (opt.isEmpty()) {
            opt = usuarioRepository.findFirstByNomeIgnoreCase(limpo);
        }
        if (opt.isEmpty()) {
            String emailGerado = UsuarioService.gerarIdentificadorEmailPorNome(limpo);
            opt = usuarioRepository.findByEmail(emailGerado);
        }

        Usuario usuario = opt.orElseThrow(() ->
                new UsernameNotFoundException("Participante não encontrado: " + identificador));

        if (usuario.getContaBloqueadaAte() != null && usuario.getContaBloqueadaAte().isAfter(LocalDateTime.now())) {
            throw new LockedException("Conta temporariamente bloqueada por excesso de tentativas falhas. Tente novamente mais tarde.");
        }

        return new CustomUserDetails(usuario);
    }
}
