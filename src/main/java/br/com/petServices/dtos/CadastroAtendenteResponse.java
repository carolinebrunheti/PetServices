package br.com.petServices.dtos;
import br.com.petServices.services.enums.Role;

public record CadastroAtendenteResponse(
    Integer id,
    String nome, 
    String email, 
    Role role
) {}
