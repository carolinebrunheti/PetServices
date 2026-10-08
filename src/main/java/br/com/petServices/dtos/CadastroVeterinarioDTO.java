package br.com.petServices.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroVeterinarioDTO(
        @NotBlank String nome,
        @NotBlank String crm,
        String especialidade,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String senha
)
 {}
