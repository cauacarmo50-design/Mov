package com.mov.repository;

import com.mov.model.HistoricoArgumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricoArgumentoRepository extends JpaRepository<HistoricoArgumento, UUID> {

    List<HistoricoArgumento> findByArgumentoIdOrderByNumeroVersaoDesc(UUID argumentoId);

    List<HistoricoArgumento> findByGrupoIdOrderByDataAlteracaoDesc(UUID grupoId);

    List<HistoricoArgumento> findAllByOrderByDataAlteracaoDesc();
}
