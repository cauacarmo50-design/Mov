package com.mov.service;

import com.mov.model.SalaDebate;
import com.mov.model.enums.StatusSala;
import com.mov.repository.SalaDebateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaDebateService {

    private final SalaDebateRepository salaDebateRepository;

    public List<SalaDebate> listarSalasAguardando() {
        return salaDebateRepository.findByStatus(StatusSala.AGUARDANDO);
    }
    
    public List<SalaDebate> listarSalasEmAndamento() {
        return salaDebateRepository.findByStatus(StatusSala.EM_ANDAMENTO);
    }
    
    public List<SalaDebate> listarTodasAtivas() {
        List<SalaDebate> ativas = salaDebateRepository.findByStatus(StatusSala.AGUARDANDO);
        ativas.addAll(salaDebateRepository.findByStatus(StatusSala.EM_ANDAMENTO));
        return ativas;
    }
}
