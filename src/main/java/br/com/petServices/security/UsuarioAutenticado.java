package br.com.petServices.security;

import br.com.petServices.services.enums.Role;

// usuario encontrado no login, seja de qual tabela for (admin, atendente, veterinario ou dono)
public record UsuarioAutenticado(
        Integer id,
        String email,
        String senha,
        Role role
) {}
