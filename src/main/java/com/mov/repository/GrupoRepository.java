package com.mov.repository;

import com.mov.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, UUID> {

    List<Grupo> findByAtivoTrueOrderByNomeAsc();

    Optional<Grupo> findByNomeIgnoreCase(String nome);

    @Query("SELECT DISTINCT g FROM Grupo g JOIN g.integrantes u WHERE u.id = :usuarioId AND g.ativo = true ORDER BY g.nome ASC")
    List<Grupo> findGruposDoUsuario(@Param("usuarioId") UUID usuarioId);
}
