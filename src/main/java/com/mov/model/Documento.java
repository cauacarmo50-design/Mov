package com.mov.model;

import com.mov.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documentos_simulacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 30)
    private TipoDocumento tipoDocumento;

    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    @Column(name = "nome_armazenado", nullable = false, length = 255)
    private String nomeArmazenado;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "publico_geral", nullable = false)
    @Builder.Default
    private Boolean publicoGeral = false;

    @Column(name = "somente_leitura", nullable = false)
    @Builder.Default
    private Boolean somenteLeitura = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enviado_por_id")
    private Usuario enviadoPor;

    @Column(name = "total_consultas", nullable = false)
    @Builder.Default
    private Integer totalConsultas = 0;

    @Column(name = "data_envio", nullable = false, updatable = false)
    private LocalDateTime dataEnvio;

    @PrePersist
    public void prePersist() {
        if (this.publicoGeral == null) this.publicoGeral = false;
        if (this.somenteLeitura == null) this.somenteLeitura = false;
        if (this.totalConsultas == null) this.totalConsultas = 0;
        if (this.dataEnvio == null) this.dataEnvio = LocalDateTime.now();
        if (this.tipoDocumento == null) this.tipoDocumento = TipoDocumento.PESQUISA;
    }

    public String getTamanhoFormatado() {
        if (tamanhoBytes == null || tamanhoBytes <= 0) return "0 KB";
        if (tamanhoBytes < 1024) return tamanhoBytes + " B";
        long kb = tamanhoBytes / 1024;
        if (kb < 1024) return kb + " KB";
        return String.format("%.1f MB", tamanhoBytes / (1024.0 * 1024.0));
    }
}
