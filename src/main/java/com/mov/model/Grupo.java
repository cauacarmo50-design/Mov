package com.mov.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "grupos_delegacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grupo {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "pais_delegacao", nullable = false, length = 100)
    private String paisDelegacao;

    @Column(length = 15)
    private String sigla;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "texto_colaborativo", columnDefinition = "TEXT")
    private String textoColaborativo;

    @Column(name = "ultimo_editor_texto", length = 100)
    private String ultimoEditorTextoNome;

    @Column(name = "data_atualizacao_texto")
    private LocalDateTime dataAtualizacaoTexto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_debate_id")
    private SalaDebate salaDebate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "grupo_integrantes",
            joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @Builder.Default
    private Set<Usuario> integrantes = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (this.ativo == null) this.ativo = true;
        if (this.dataCriacao == null) this.dataCriacao = LocalDateTime.now();
        if (this.integrantes == null) this.integrantes = new HashSet<>();
    }

    public boolean contemIntegrante(UUID usuarioId) {
        if (usuarioId == null || integrantes == null) return false;
        return integrantes.stream().anyMatch(u -> usuarioId.equals(u.getId()));
    }
}
