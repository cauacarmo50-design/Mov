package com.mov.repository;

import com.mov.model.SalaDebate;
import com.mov.model.enums.StatusSala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaDebateRepository extends JpaRepository<SalaDebate, UUID> {
    List<SalaDebate> findByStatus(StatusSala status);
}
