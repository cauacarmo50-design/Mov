package com.mov.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoDocumento {
    POSICAO("Documento de Posição da Delegação"),
    PESQUISA("Material de Pesquisa"),
    OFICIAL("Documento Oficial da Simulação"),
    RESOLUCAO("Projeto de Resolução"),
    REFERENCIA("Referência / Material Complementar");

    private final String rotulo;
}
