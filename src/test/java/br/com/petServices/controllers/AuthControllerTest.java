package br.com.petServices.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.petServices.config.SecurityConfig;
import br.com.petServices.dtos.LoginDTO;
import br.com.petServices.dtos.TokenResponse;
import br.com.petServices.exceptions.MuitasTentativasException;
import br.com.petServices.security.TokenTeste;
import br.com.petServices.services.AuthService;

@WebMvcTest(controllers = AuthController.class, properties = "jwt.secret=" + TokenTeste.SECRET)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginDeveRetornar200ComOTokenSemPrecisarEstarLogado() throws Exception {
        //arrange
        when(authService.login(any(LoginDTO.class), anyString()))
                .thenReturn(new TokenResponse("abc.def.ghi", "Bearer", Instant.parse("2026-10-22T18:00:00Z")));

        String json = """
                {
                  "email": "admin@petservices.com",
                  "senha": "123456"
                }
                """;

        //act + assert: sem header Authorization, a rota de login e publica
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc.def.ghi"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void loginDeveRetornar401QuandoEmailOuSenhaEstaoErrados() throws Exception {
        //arrange
        when(authService.login(any(LoginDTO.class), anyString()))
                .thenThrow(new BadCredentialsException("Email ou senha inválidos"));

        String json = """
                {
                  "email": "admin@petservices.com",
                  "senha": "errada"
                }
                """;

        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Email ou senha inválidos"));
    }

    @Test
    void loginDeveRetornar429QuandoOIpEstaBloqueado() throws Exception {
        //arrange
        when(authService.login(any(LoginDTO.class), anyString()))
                .thenThrow(new MuitasTentativasException("Muitas tentativas de login. Tente novamente em alguns minutos"));

        String json = """
                {
                  "email": "admin@petservices.com",
                  "senha": "123456"
                }
                """;

        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void loginDeveRetornar400QuandoOEmailEhInvalido() throws Exception {
        //arrange
        String json = """
                {
                  "email": "nao-e-email",
                  "senha": ""
                }
                """;

        //act + assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(), any());
    }
}
