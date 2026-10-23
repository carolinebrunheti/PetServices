package br.com.petServices.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.petServices.dtos.TokenResponse;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long expiracaoMinutos;

    public TokenService(JwtEncoder jwtEncoder, @Value("${jwt.expiracao-minutos:120}") long expiracaoMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public TokenResponse gerar(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expiracaoMinutos, ChronoUnit.MINUTES);

        // a senha nunca vai para o token: so quem e (email e id) e o que pode fazer (role)
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("petServices")
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.email())
                .claim("id", usuario.id())
                .claim("role", usuario.role().name())
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", expiraEm);
    }
}
