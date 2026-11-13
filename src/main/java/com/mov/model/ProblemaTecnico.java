package com.mov.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "problemas_tecnicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemaTecnico {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "pagina_origem", length = 200)
    private String paginaOrigem;

    @Column(name = "dispositivo_info", length = 200)
    private String dispositivoInfo;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ABERTO";

    @Column(name = "resposta_equipe", columnDefinition = "TEXT")
    private String respostaEquipe;

    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = "ABERTO";
        if (this.dataRegistro == null) this.dataRegistro = LocalDateTime.now();
    }
}
