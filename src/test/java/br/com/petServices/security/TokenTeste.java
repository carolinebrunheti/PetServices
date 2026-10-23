package br.com.petServices.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import br.com.petServices.services.enums.Role;

// ajuda os testes de controller a simular um usuario logado, sem precisar gerar um token de verdade
public final class TokenTeste {

    // usado no @WebMvcTest(properties = ...) para o SecurityConfig subir
    public static final String SECRET = "segredo-so-para-testes-com-mais-de-32-caracteres";

    private TokenTeste() {}

    public static JwtRequestPostProcessor usuario(Role role, int id) {
        return jwt()
                .jwt(token -> token
                        .subject(role.name().toLowerCase() + "@teste.com")
                        .claim("id", id)
                        .claim("role", role.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
