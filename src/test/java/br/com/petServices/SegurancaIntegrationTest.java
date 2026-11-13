package br.com.petServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Seguranca com a aplicacao real (filtros do Spring Security, JwtDecoder, banco):
 * tokens invalidos, papeis sem permissao, um tutor tentando ver dados de outro e ataques ao login.
 * Mesma configuracao do PetServicesApplicationTests: o Spring reaproveita o contexto e os conteineres.
 */
@SpringBootTest(properties = {
        "jwt.secret=" + PetServicesApplicationTests.SEGREDO,
        "admin-inicial.email=" + PetServicesApplicationTests.ADMIN_EMAIL,
        "admin-inicial.senha=" + PetServicesApplicationTests.ADMIN_SENHA
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SegurancaIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Nested
    class TokenInvalido {

        @Test
        void tokenExpiradoDa401() throws Exception {
            String token = token("ADMIN", 1, Instant.now().minusSeconds(600), Instant.now().minusSeconds(120),
                    PetServicesApplicationTests.SEGREDO);
            mockMvc.perform(get("/dono").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenRecemExpiradoAindaPassaPelaToleranciaDeRelogio() throws Exception {
            // o Spring aceita ate 60 s de diferenca de relogio entre quem emite e quem valida:
            // um token vencido ha 30 s ainda passa. Documenta o comportamento padrao.
            String token = token("ADMIN", 1, Instant.now().minusSeconds(600), Instant.now().minusSeconds(30),
                    PetServicesApplicationTests.SEGREDO);
            mockMvc.perform(get("/dono").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        void tokenAssinadoComOutroSegredoDa401() throws Exception {
            String token = token("ADMIN", 1, Instant.now(), Instant.now().plusSeconds(300),
                    "um-segredo-que-o-atacante-inventou-com-32-chars");
            mockMvc.perform(get("/dono").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenSemAssinaturaAlgNoneDa401() throws Exception {
            // ataque classico: dizer no cabecalho que o token "nao tem assinatura"
            String cabecalho = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
            String corpo = base64Url("{\"sub\":\"hacker@teste.dev\",\"id\":1,\"role\":\"ADMIN\",\"exp\":"
                    + Instant.now().plusSeconds(300).getEpochSecond() + "}");
            mockMvc.perform(get("/dono").header("Authorization", "Bearer " + cabecalho + "." + corpo + "."))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenAdulteradoDeTutorParaAdminDa401() throws Exception {
            // pega um token valido de TUTOR, troca o papel para ADMIN e mantem a assinatura original
            String[] partes = tokenValido("TUTOR", 1).split("\\.");
            String corpo = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                    .replace("\"TUTOR\"", "\"ADMIN\"");
            String adulterado = partes[0] + "." + base64Url(corpo) + "." + partes[2];

            mockMvc.perform(get("/dono").header("Authorization", "Bearer " + adulterado))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenAssinadoMasSemIdERoleDa401() throws Exception {
            var chave = new SecretKeySpec(PetServicesApplicationTests.SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            var claims = JwtClaimsSet.builder().subject("sem-claims@teste.dev")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            String token = NimbusJwtEncoder.withSecretKey(chave).build()
                    .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

            // GET /pet so exige "autenticado" e o controller le id e role do token:
            // sem essas claims o token e recusado na entrada (antes dava 500 com NullPointerException)
            mockMvc.perform(get("/pet").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenComRoleInventadoDa401() throws Exception {
            mockMvc.perform(get("/pet").header("Authorization", "Bearer " + tokenValido("SUPERUSUARIO", 1)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void textoQualquerNoLugarDoTokenDa401() throws Exception {
            mockMvc.perform(get("/dono").header("Authorization", "Bearer nao-sou-um-jwt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class PapelSemPermissao {

        @Test
        void tutorNaoListaDonos() throws Exception {
            mockMvc.perform(get("/dono").header("Authorization", "Bearer " + tokenValido("TUTOR", 1)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensagem").value("Acesso negado: você não tem permissão para este recurso"));
        }

        @Test
        void veterinarioNaoCadastraDono() throws Exception {
            mockMvc.perform(post("/dono").header("Authorization", "Bearer " + tokenValido("VETERINARIO", 1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"email\":\"x@teste.dev\",\"senha\":\"123456\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void atendenteNaoCriaAdmin() throws Exception {
            mockMvc.perform(post("/admin").header("Authorization", "Bearer " + tokenValido("ATENDENTE", 1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"email\":\"novo-admin@teste.dev\",\"senha\":\"senha-bem-comprida\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tutorNaoCadastraPet() throws Exception {
            mockMvc.perform(post("/pet").header("Authorization", "Bearer " + tokenValido("TUTOR", 1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Rex\",\"especie\":\"CACHORRO\",\"donoId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void tutorNaoVePetDeOutroTutor() throws Exception {
            String admin = tokenValido("ADMIN", 1);
            int donoA = idDaResposta(criarDono(admin, "dono-a@teste.dev"));
            int donoB = idDaResposta(criarDono(admin, "dono-b@teste.dev"));
            int petDeB = idDaResposta(mockMvc.perform(post("/pet").header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Mia\",\"especie\":\"GATO\",\"donoId\":" + donoB + "}"))
                    .andExpect(status().isCreated()).andReturn().getResponse());

            // o dono B ve o proprio pet
            mockMvc.perform(get("/pet/" + petDeB).header("Authorization", "Bearer " + tokenValido("TUTOR", donoB)))
                    .andExpect(status().isOk());
            // o dono A, trocando o id na URL, nao ve
            mockMvc.perform(get("/pet/" + petDeB).header("Authorization", "Bearer " + tokenValido("TUTOR", donoA)))
                    .andExpect(status().isForbidden());
            // e na listagem o pet de B nao aparece para A
            mockMvc.perform(get("/pet").header("Authorization", "Bearer " + tokenValido("TUTOR", donoA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.id == " + petDeB + ")]").isEmpty());
        }
    }

    @Nested
    class Login {

        @Test
        void depoisDe5FalhasBloqueiaMesmoComASenhaCerta() throws Exception {
            String ip = "10.0.0.51";
            for (int i = 1; i <= 5; i++) {
                mockMvc.perform(login(ip, PetServicesApplicationTests.ADMIN_EMAIL, "chute-" + i))
                        .andExpect(status().isUnauthorized());
            }
            mockMvc.perform(login(ip, PetServicesApplicationTests.ADMIN_EMAIL, PetServicesApplicationTests.ADMIN_SENHA))
                    .andExpect(status().isTooManyRequests());

            // outro IP continua conseguindo entrar
            mockMvc.perform(login("10.0.0.52", PetServicesApplicationTests.ADMIN_EMAIL, PetServicesApplicationTests.ADMIN_SENHA))
                    .andExpect(status().isOk());
        }

        @Test
        void emailInexistenteESenhaErradaTemAMesmaResposta() throws Exception {
            MockHttpServletResponse emailInexistente = mockMvc.perform(
                    login("10.0.0.53", "ninguem@teste.dev", "qualquer")).andReturn().getResponse();
            MockHttpServletResponse senhaErrada = mockMvc.perform(
                    login("10.0.0.54", PetServicesApplicationTests.ADMIN_EMAIL, "qualquer")).andReturn().getResponse();

            // nao da para descobrir quais emails estao cadastrados
            assertThat(emailInexistente.getStatus()).isEqualTo(401).isEqualTo(senhaErrada.getStatus());
            assertThat(mensagem(emailInexistente)).isEqualTo(mensagem(senhaErrada));
        }

        @Test
        void tokenDoLoginTemOsDadosCertosENaoTemSenha() throws Exception {
            String resposta = mockMvc.perform(login("10.0.0.55", PetServicesApplicationTests.ADMIN_EMAIL,
                            PetServicesApplicationTests.ADMIN_SENHA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andReturn().getResponse().getContentAsString();
            String token = resposta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
            String cabecalho = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
            String corpo = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

            assertThat(cabecalho).contains("\"alg\":\"HS256\"");
            assertThat(corpo).contains("\"role\":\"ADMIN\"").contains("\"sub\":\"" + PetServicesApplicationTests.ADMIN_EMAIL + "\"")
                    .doesNotContain(PetServicesApplicationTests.ADMIN_SENHA).doesNotContainIgnoringCase("senha");
        }

        @Test
        void respostasDeCadastroNaoExpoemSenhaNemHash() throws Exception {
            String admin = tokenValido("ADMIN", 1);
            String criado = criarDono(admin, "sem-hash@teste.dev").getContentAsString();
            String consultado = mockMvc.perform(get("/dono/" + idDoJson(criado))
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

            for (String json : new String[] { criado, consultado }) {
                assertThat(json).doesNotContainIgnoringCase("senha").doesNotContain("123456").doesNotContain("$2a$");
            }
        }
    }

    // ---------- ajudantes ----------

    private MockHttpServletResponse criarDono(String token, String email) throws Exception {
        return mockMvc.perform(post("/dono").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dono Teste\",\"email\":\"" + email + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse();
    }

    private int idDaResposta(MockHttpServletResponse resposta) throws Exception {
        return idDoJson(resposta.getContentAsString());
    }

    private int idDoJson(String json) {
        return Integer.parseInt(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private String mensagem(MockHttpServletResponse resposta) throws Exception {
        return resposta.getContentAsString().replaceAll(".*\"mensagem\":\"([^\"]*)\".*", "$1");
    }

    // o limite de tentativas e por IP: cada teste usa o seu para nao interferir nos outros
    private MockHttpServletRequestBuilder login(String ip, String email, String senha) {
        return post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
                .with(req -> {
                    req.setRemoteAddr(ip);
                    return req;
                });
    }

    private String tokenValido(String role, int id) {
        return token(role, id, Instant.now(), Instant.now().plusSeconds(300), PetServicesApplicationTests.SEGREDO);
    }

    // gera um token igual ao do petServices: HS256 com as claims "id" e "role"
    private String token(String role, int id, Instant emitidoEm, Instant expiraEm, String segredo) {
        var chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var claims = JwtClaimsSet.builder()
                .issuer("petServices")
                .subject("teste@petservices.dev")
                .claim("id", id)
                .claim("role", role)
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .build();
        return NimbusJwtEncoder.withSecretKey(chave).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
