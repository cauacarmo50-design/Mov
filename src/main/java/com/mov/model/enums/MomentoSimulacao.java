package com.mov.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MomentoSimulacao {
    ANTES_EVENTO("Antes do Evento (Preparação)"),
    DURANTE_EVENTO("Durante o Evento (Debate Presencial)"),
    DEPOIS_EVENTO("Depois do Evento (Consulta e Avaliação)");

    private final String descricao;
}
