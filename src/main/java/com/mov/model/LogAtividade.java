package com.mov.model;

import com.mov.model.enums.MomentoSimulacao;
import com.mov.model.enums.TipoEventoLog;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logs_atividade_piloto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAtividade {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoLog tipoEvento;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "usuario_id", columnDefinition = "VARCHAR(36)")
    private UUID usuarioId;

    @Column(name = "usuario_nome", length = 120)
    private String usuarioNome;

    @Column(name = "usuario_perfil", length = 30)
    private String usuarioPerfil;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "grupo_id", columnDefinition = "VARCHAR(36)")
    private UUID grupoId;

    @Column(name = "grupo_nome", length = 150)
    private String grupoNome;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "recurso_id", columnDefinition = "VARCHAR(36)")
    private UUID recursoId;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MomentoSimulacao momento;

    @Column(length = 60)
    private String dispositivo;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @PrePersist
    public void prePersist() {
        if (this.dataHora == null) this.dataHora = LocalDateTime.now();
    }
}
