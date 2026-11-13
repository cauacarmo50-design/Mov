package com.mov.repository;

import com.mov.model.ArgumentoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArgumentoRegistroRepository extends JpaRepository<ArgumentoRegistro, UUID> {

    List<ArgumentoRegistro> findByGrupoIdOrderByDataAtualizacaoDesc(UUID grupoId);

    List<ArgumentoRegistro> findByCompartilhadoPlenarioTrueOrderByDataAtualizacaoDesc();

    List<ArgumentoRegistro> findAllByOrderByDataAtualizacaoDesc();

    long countByGrupoId(UUID grupoId);

    @Query("SELECT COUNT(a) FROM ArgumentoRegistro a WHERE a.fonte IS NOT NULL AND TRIM(a.fonte) <> ''")
    long countComFonte();

    @Query("SELECT COUNT(a) FROM ArgumentoRegistro a WHERE a.contraponto IS NOT NULL AND TRIM(a.contraponto) <> ''")
    long countComContraponto();
}
