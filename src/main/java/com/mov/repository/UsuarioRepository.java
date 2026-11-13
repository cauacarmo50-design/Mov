package com.mov.repository;

import com.mov.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findFirstByNomeIgnoreCase(String nome);
    Optional<Usuario> findByTokenAtivacao(String tokenAtivacao);
}
