package com.mov.service;

import com.mov.dto.UsuarioCadastroRecord;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoUsuario;
import com.mov.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CaptchaService captchaService;

    @Value("${APP_URL:http://localhost:8080}")
    private String appUrl;

    public static String gerarIdentificadorEmailPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "participante." + UUID.randomUUID().toString().substring(0, 6) + "@senac.mov";
        }
        String semAcento = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = semAcento.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", ".");
        if (slug.isEmpty()) {
            slug = "participante." + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug + "@senac.mov";
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional
    public Usuario registrarUsuario(UsuarioCadastroRecord dto, String clientIp) {
        if (!captchaService.isCaptchaValid(dto.cfTurnstileResponse(), clientIp)) {
            throw new IllegalArgumentException("Validação de segurança (CAPTCHA) falhou. Tente novamente.");
        }

        String nomeLimpo = dto.nome().trim();
        String emailFinal = (dto.email() != null && !dto.email().isBlank())
                ? dto.email().trim().toLowerCase()
                : gerarIdentificadorEmailPorNome(nomeLimpo);

        if (usuarioRepository.findByEmail(emailFinal).isPresent()) {
            throw new IllegalArgumentException("Já existe um participante cadastrado com este nome ou e-mail.");
        }

        var usuario = new Usuario();
        usuario.setNome(nomeLimpo);
        usuario.setEmail(emailFinal);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setInstituicao("Senac SP");
        usuario.setAtivo(true);
        usuario.setTipo(TipoUsuario.DELEGADO);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario criarOuAtualizarParticipanteOrganizacao(
            String nome,
            String email,
            String senha,
            TipoUsuario tipo,
            String instituicao
    ) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome completo do participante é obrigatório.");
        }
        String nomeLimpo = nome.trim();
        String emailLimpo = (email != null && !email.isBlank())
                ? email.trim().toLowerCase()
                : gerarIdentificadorEmailPorNome(nomeLimpo);

        Optional<Usuario> existente = usuarioRepository.findByEmail(emailLimpo);
        if (existente.isEmpty()) {
            existente = usuarioRepository.findFirstByNomeIgnoreCase(nomeLimpo);
        }
        if (existente.isPresent()) {
            Usuario u = existente.get();
            u.setNome(nomeLimpo);
            if (tipo != null) u.setTipo(tipo);
            if (instituicao != null && !instituicao.isBlank()) u.setInstituicao(instituicao.trim());
            if (senha != null && !senha.isBlank()) {
                u.setSenha(passwordEncoder.encode(senha));
            }
            u.setAtivo(true);
            u.setContaBloqueadaAte(null);
            u.setTentativasFalhasLogin(0);
            return usuarioRepository.save(u);
        }

        String senhaFinal = (senha != null && !senha.isBlank()) ? senha : "Senac@2026";
        Usuario novo = Usuario.builder()
                .nome(nomeLimpo)
                .email(emailLimpo)
                .senha(passwordEncoder.encode(senhaFinal))
                .instituicao(instituicao != null && !instituicao.isBlank() ? instituicao.trim() : "Senac SP")
                .tipo(tipo != null ? tipo : TipoUsuario.DELEGADO)
                .ativo(true)
                .nivel(1)
                .consensosAlcancados(0)
                .debatesParticipados(0)
                .tentativasFalhasLogin(0)
                .build();
        return usuarioRepository.save(novo);
    }

    @Transactional
    public void redefinirSenha(UUID usuarioId, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 4) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 4 caracteres.");
        }
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        u.setSenha(passwordEncoder.encode(novaSenha));
        u.setTentativasFalhasLogin(0);
        u.setContaBloqueadaAte(null);
        u.setAtivo(true);
        usuarioRepository.save(u);
    }

    @Transactional
    public void alterarPerfil(UUID usuarioId, TipoUsuario novoTipo) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        u.setTipo(novoTipo);
        usuarioRepository.save(u);
    }

    @Transactional
    public Usuario atualizarDadosProprios(UUID usuarioId, String nome, String instituicao) {
        if (nome == null || nome.trim().length() < 2) {
            throw new IllegalArgumentException("O nome deve ter pelo menos 2 caracteres.");
        }
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        u.setNome(nome.trim());
        if (instituicao != null && !instituicao.isBlank()) {
            u.setInstituicao(instituicao.trim());
        }
        return usuarioRepository.save(u);
    }

    @Transactional
    public void alterarSenhaPropria(UUID usuarioId, String senhaAtual, String novaSenha, String confirmarSenha) {
        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new IllegalArgumentException("Informe sua senha atual.");
        }
        if (novaSenha == null || novaSenha.length() < 4) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 4 caracteres.");
        }
        if (!novaSenha.equals(confirmarSenha)) {
            throw new IllegalArgumentException("A confirmação da nova senha não confere.");
        }
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!passwordEncoder.matches(senhaAtual, u.getSenha())) {
            throw new IllegalArgumentException("A senha atual informada está incorreta.");
        }
        u.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(u);
    }

    @Transactional
    public boolean confirmarConta(String token) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenAtivacao(token);
        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getTokenAtivacaoExpira() != null && usuario.getTokenAtivacaoExpira().isBefore(LocalDateTime.now())) {
            return false;
        }

        usuario.setAtivo(true);
        usuario.setTokenAtivacao(null);
        usuario.setTokenAtivacaoExpira(null);
        usuarioRepository.save(usuario);
        return true;
    }
}
