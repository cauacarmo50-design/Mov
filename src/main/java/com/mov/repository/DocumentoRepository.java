package com.mov.repository;

import com.mov.model.Documento;
import com.mov.model.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByPublicoGeralTrueOrderByDataEnvioDesc();

    List<Documento> findByGrupoIdOrderByDataEnvioDesc(UUID grupoId);

    List<Documento> findByTipoDocumentoOrderByDataEnvioDesc(TipoDocumento tipoDocumento);

    List<Documento> findAllByOrderByDataEnvioDesc();

    long countByGrupoId(UUID grupoId);
}
