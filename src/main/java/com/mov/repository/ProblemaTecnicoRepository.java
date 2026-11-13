package com.mov.repository;

import com.mov.model.ProblemaTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProblemaTecnicoRepository extends JpaRepository<ProblemaTecnico, UUID> {

    List<ProblemaTecnico> findAllByOrderByDataRegistroDesc();

    long countByStatus(String status);
}
