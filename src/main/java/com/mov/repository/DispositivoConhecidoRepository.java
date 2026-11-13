package com.mov.repository;

import com.mov.model.DispositivoConhecido;
import com.mov.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DispositivoConhecidoRepository extends JpaRepository<DispositivoConhecido, UUID> {
    Optional<DispositivoConhecido> findByUsuarioAndIpAddressAndUserAgent(Usuario usuario, String ipAddress, String userAgent);
}
