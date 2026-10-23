package br.com.petServices.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import br.com.petServices.config.SecurityConfig;
import br.com.petServices.dtos.TokenResponse;
import br.com.petServices.services.enums.Role;

// usa o encoder e o decoder de verdade do SecurityConfig, sem mock
class TokenServiceTest {

    private final SecurityConfig securityConfig = new SecurityConfig(TokenTeste.SECRET);
    private final TokenService tokenService = new TokenService(securityConfig.jwtEncoder(), 120);
    private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder();

    private UsuarioAutenticado admin() {
        return new UsuarioAutenticado(7, "admin@petservices.com", "hash-qualquer", Role.ADMIN);
    }

    @Test
    @DisplayName("gerar: o token deve trazer email, id, role e expirar em 2 horas")
    void gerarTokenComOsDadosDoUsuario() {
        // act
        TokenResponse resposta = tokenService.gerar(admin());
        Jwt jwt = jwtDecoder.decode(resposta.token());

        // assert
        assertEquals("Bearer", resposta.tipo());
        assertEquals("admin@petservices.com", jwt.getSubject());
        assertEquals(7, ((Number) jwt.getClaim("id")).intValue());
        assertEquals("ADMIN", jwt.getClaimAsString("role"));

        Duration validade = Duration.between(Instant.now(), resposta.expiraEm());
        assertTrue(validade.toMinutes() >= 119 && validade.toMinutes() <= 120);
    }

    @Test
    @DisplayName("gerar: a senha nunca deve ir para dentro do token")
    void tokenNaoTemSenha() {
        // act
        Jwt jwt = jwtDecoder.decode(tokenService.gerar(admin()).token());

        // assert
        assertFalse(jwt.getClaims().containsKey("senha"));
        assertFalse(jwt.getTokenValue().contains("hash-qualquer"));
    }

    @Test
    @DisplayName("decode: token assinado com outro segredo deve ser rejeitado")
    void tokenDeOutroSegredoEhRejeitado() {
        // arrange: alguem gerou um token com uma chave diferente da nossa
        SecurityConfig outraChave = new SecurityConfig("outro-segredo-qualquer-com-mais-de-32-caracteres");
        String tokenFalso = new TokenService(outraChave.jwtEncoder(), 120).gerar(admin()).token();

        // act + assert
        assertThrows(JwtException.class, () -> jwtDecoder.decode(tokenFalso));
    }

    @Test
    @DisplayName("SecurityConfig: deve recusar segredo com menos de 32 caracteres")
    void segredoCurtoEhRecusado() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfig("curto"));
    }
}
