package com.mov.repository;

import com.mov.model.LogAtividade;
import com.mov.model.enums.TipoEventoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LogAtividadeRepository extends JpaRepository<LogAtividade, UUID> {

    List<LogAtividade> findTop200ByOrderByDataHoraDesc();

    List<LogAtividade> findAllByOrderByDataHoraDesc();

    long countByTipoEvento(TipoEventoLog tipoEvento);

    @Query("SELECT COUNT(DISTINCT l.usuarioId) FROM LogAtividade l WHERE l.usuarioId IS NOT NULL")
    long countUsuariosUnicosComAcesso();

    @Query("SELECT COUNT(DISTINCT l.usuarioId) FROM LogAtividade l WHERE l.usuarioId IS NOT NULL AND l.tipoEvento IN :eventosContribuicao")
    long countUsuariosComContribuicao(@Param("eventosContribuicao") List<TipoEventoLog> eventosContribuicao);

    @Query("SELECT COUNT(DISTINCT l.usuarioId) FROM LogAtividade l WHERE l.grupoId = :grupoId AND l.usuarioId IS NOT NULL")
    long countUsuariosAtivosNoGrupo(@Param("grupoId") UUID grupoId);
}
