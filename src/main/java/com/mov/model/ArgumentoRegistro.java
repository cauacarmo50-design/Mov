package com.mov.model;

import com.mov.model.enums.MomentoSimulacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "argumentos_registros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArgumentoRegistro {

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
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @Column(nullable = false, length = 120)
    private String delegacao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String argumento;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(length = 300)
    private String fonte;

    @Column(name = "link_fonte", length = 500)
    private String linkFonte;

    @Column(columnDefinition = "TEXT")
    private String contraponto;

    @Column(name = "autor_contraponto_nome", length = 120)
    private String autorContrapontoNome;

    @Column(name = "delegacao_contraponto", length = 120)
    private String delegacaoContraponto;

    @Column(name = "compartilhado_plenario", nullable = false)
    @Builder.Default
    private Boolean compartilhadoPlenario = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MomentoSimulacao momento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ultimo_editor_id")
    private Usuario ultimoEditor;

    @Column(name = "numero_versao", nullable = false)
    @Builder.Default
    private Integer numeroVersao = 1;

    @Version
    @Column(name = "versao_lock")
    private Long versaoLock;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        if (this.dataCriacao == null) this.dataCriacao = agora;
        if (this.dataAtualizacao == null) this.dataAtualizacao = agora;
        if (this.numeroVersao == null) this.numeroVersao = 1;
        if (this.compartilhadoPlenario == null) this.compartilhadoPlenario = false;
        if (this.momento == null) this.momento = MomentoSimulacao.ANTES_EVENTO;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}
