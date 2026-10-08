package br.com.petServices.dtos;

import br.com.petServices.services.enums.Role;

public record CadastroDonoResponse(
    Integer id, 
    String nome, 
    String email, 
    Role role) {}
