package br.com.petServices.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecretKey chave;

    public SecurityConfig(@Value("${jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 exige uma chave de pelo menos 256 bits (32 bytes)
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret precisa ter pelo menos 32 caracteres");
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {

        // repassa os erros de seguranca para o GlobalExceptionHandler,
        // assim o 401/403 sai no mesmo formato ErroResponse do resto da API
        AuthenticationEntryPoint naoAutenticado = (request, response, ex) -> resolver.resolveException(request, response, null, ex);
        AccessDeniedHandler semPermissao = (request, response, ex) -> resolver.resolveException(request, response, null, ex);

        http
                // API stateless com token no header: nao usa cookie de sessao, entao CSRF nao se aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // health check do Kubernetes (so diz UP/DOWN, sem detalhes)
                        .requestMatchers("/actuator/health/**").permitAll()

                        // so o admin cria, edita e remove usuarios
                        .requestMatchers("/admin/**", "/atendente/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/veterinario/**").authenticated()
                        .requestMatchers("/veterinario/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/dono/**").hasAnyRole("ADMIN", "ATENDENTE", "VETERINARIO")
                        .requestMatchers("/dono/**").hasRole("ADMIN")

                        // o tutor tambem consulta pets, mas o controller filtra para ver so os dele
                        .requestMatchers(HttpMethod.GET, "/pet/**").authenticated()
                        .requestMatchers("/pet/**").hasAnyRole("ADMIN", "ATENDENTE")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // transforma a claim "role" do token (ex.: "ADMIN") na permissao ROLE_ADMIN usada pelo hasRole
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter permissoes = new JwtGrantedAuthoritiesConverter();
        permissoes.setAuthoritiesClaimName("role");
        permissoes.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(permissoes);
        return converter;
    }
}
