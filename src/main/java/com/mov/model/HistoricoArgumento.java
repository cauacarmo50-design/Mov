package com.mov.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_argumentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoArgumento {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "argumento_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID argumentoId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "grupo_id", columnDefinition = "VARCHAR(36)")
    private UUID grupoId;

    @Column(name = "numero_versao", nullable = false)
    private Integer numeroVersao;

    @Column(name = "tipo_alteracao", nullable = false, length = 40)
    private String tipoAlteracao;

    @Column(length = 120)
    private String delegacao;

    @Column(columnDefinition = "TEXT")
    private String argumento;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(length = 300)
    private String fonte;

    @Column(name = "link_fonte", length = 500)
    private String linkFonte;

    @Column(columnDefinition = "TEXT")
    private String contraponto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_alteracao_id")
    private Usuario autorAlteracao;

    @Column(name = "data_alteracao", nullable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    @PrePersist
    public void prePersist() {
        if (this.dataAlteracao == null) this.dataAlteracao = LocalDateTime.now();
    }
}
