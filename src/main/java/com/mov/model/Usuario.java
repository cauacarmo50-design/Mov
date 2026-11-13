package com.mov.model;

import com.mov.model.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.mov.validation.SenhaForte;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "O e-mail deve ser válido")
    @Size(max = 150, message = "O e-mail não pode exceder 150 caracteres")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(columnDefinition = "TEXT")
    private String biografia;

    @Column(length = 150)
    private String instituicao;

    private Integer nivel;

    @Column(name = "consensos_alcancados")
    private Integer consensosAlcancados;

    @Column(name = "debates_participados")
    private Integer debatesParticipados;

    @Column(nullable = false)
    private Boolean ativo = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUsuario tipo;

    @Column(name = "token_ativacao")
    private String tokenAtivacao;

    @Column(name = "token_ativacao_expira")
    private LocalDateTime tokenAtivacaoExpira;

    @Column(name = "tentativas_falhas_login")
    private Integer tentativasFalhasLogin;

    @Column(name = "conta_bloqueada_ate")
    private LocalDateTime contaBloqueadaAte;

    @PrePersist
    public void prePersist() {
        if (this.nivel == null) this.nivel = 1;
        if (this.consensosAlcancados == null) this.consensosAlcancados = 0;
        if (this.debatesParticipados == null) this.debatesParticipados = 0;
        if (this.ativo == null) this.ativo = false;
        if (this.tipo == null) this.tipo = TipoUsuario.DELEGADO;
        if (this.tentativasFalhasLogin == null) this.tentativasFalhasLogin = 0;
    }
}
