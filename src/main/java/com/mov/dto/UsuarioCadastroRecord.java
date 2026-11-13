package com.mov.dto;

import com.mov.validation.SenhaForte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroRecord(
        @NotBlank(message = "O nome completo é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String nome,

        String email,

        @NotBlank(message = "A senha é obrigatória")
        @SenhaForte
        String senha,
        
        String cfTurnstileResponse
) {}
