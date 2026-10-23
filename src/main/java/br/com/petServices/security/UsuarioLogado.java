package br.com.petServices.security;

import org.springframework.security.oauth2.jwt.Jwt;

import br.com.petServices.services.enums.Role;

// dados de quem fez a requisicao, lidos do token
public record UsuarioLogado(
        Integer id,
        String email,
        Role role
) {
    public static UsuarioLogado de(Jwt jwt) {
        Number id = jwt.getClaim("id");
        return new UsuarioLogado(id.intValue(), jwt.getSubject(), Role.valueOf(jwt.getClaimAsString("role")));
    }

    public boolean isTutor() {
        return role == Role.TUTOR;
    }
}
