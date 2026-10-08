package br.com.petServices.dtos;

import br.com.petServices.services.enums.Role;

public record CadastroVeterinarioResponse(
        Integer id,
        String nome,
        String crm,
        String especialidade,
        String email,
        Role role
){}
