package br.com.petServices.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.petServices.config.SecurityConfig;
import br.com.petServices.dtos.CadastroVeterinarioDTO;
import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.security.TokenTeste;
import br.com.petServices.services.CadastroVeterinarioService;
import br.com.petServices.services.enums.Role;

// sobe so a camada web (controller + GlobalExceptionHandler + validacao + seguranca),
// sem banco, sem kafka e sem os services de verdade
@WebMvcTest(controllers = CadastroVeterinarioController.class, properties = "jwt.secret=" + TokenTeste.SECRET)
@Import(SecurityConfig.class)
class CadastroVeterinarioControllerTest {

    // "cliente HTTP falso": faz requisicoes no controller sem subir servidor
    @Autowired
    private MockMvc mockMvc;

    // mock do service colocado no contexto do Spring (no lugar do @Mock do teste unitario)
    @MockitoBean
    private CadastroVeterinarioService cadastroVeterinarioService;

    @Test
    void salvarDeveRetornar201ComOVeterinarioCriado() throws Exception {
        //arrange
        CadastroVeterinarioResponse salvo =
                new CadastroVeterinarioResponse(1, "Ana", "CRM-123", "Felinos", "ana@vet.com", Role.VETERINARIO);
        when(cadastroVeterinarioService.salvar(any(CadastroVeterinarioDTO.class))).thenReturn(salvo);

        // corpo da requisicao, igual ao que voce manda no Postman
        String json = """
                {
                  "nome": "Ana",
                  "crm": "CRM-123",
                  "especialidade": "Felinos",
                  "email": "ana@vet.com",
                  "senha": "123456"
                }
                """;

        //act + assert
        mockMvc.perform(post("/veterinario")
                        .with(TokenTeste.usuario(Role.ADMIN, 1))   // so o admin cadastra
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())                       // status 201
                .andExpect(jsonPath("$.id").value(1))                  // campos do JSON de resposta
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.role").value("VETERINARIO"));

        verify(cadastroVeterinarioService).salvar(any(CadastroVeterinarioDTO.class));
    }

    @Test
    void salvarDeveRetornar400QuandoOsDadosSaoInvalidos() throws Exception {
        //arrange: nome vazio, email invalido e senha curta (quebra o @Valid do DTO)
        String json = """
                {
                  "nome": "",
                  "crm": "CRM-123",
                  "email": "email-invalido",
                  "senha": "123"
                }
                """;

        //act + assert
        mockMvc.perform(post("/veterinario")
                        .with(TokenTeste.usuario(Role.ADMIN, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos na requisição"));

        // a validacao barra antes de chegar no service
        verify(cadastroVeterinarioService, never()).salvar(any());
    }

    @Test
    void buscarPorIdDeveRetornar404QuandoNaoExiste() throws Exception {
        //arrange: o service lanca a excecao e o GlobalExceptionHandler transforma em 404
        when(cadastroVeterinarioService.buscarPorId(99))
                .thenThrow(new RecursoNaoEncontradoException("Veterinário não encontrado com o ID: 99"));

        //act + assert
        mockMvc.perform(get("/veterinario/99")
                        .with(TokenTeste.usuario(Role.TUTOR, 5)))   // qualquer usuario logado pode consultar
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Veterinário não encontrado com o ID: 99"));
    }

    @Test
    void buscarSemTokenDeveRetornar401() throws Exception {
        //act + assert
        mockMvc.perform(get("/veterinario"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(cadastroVeterinarioService, never()).buscarTodos(any());
    }

    @Test
    void buscarComTokenInvalidoDeveRetornar401() throws Exception {
        //act + assert: aqui o token passa pelo JwtDecoder de verdade e e rejeitado
        mockMvc.perform(get("/veterinario")
                        .header("Authorization", "Bearer token-inventado"))
                .andExpect(status().isUnauthorized());

        verify(cadastroVeterinarioService, never()).buscarTodos(any());
    }

    @Test
    void veterinarioNaoPodeCadastrarOutroDeveRetornar403() throws Exception {
        //arrange
        String json = """
                {
                  "nome": "Ana",
                  "crm": "CRM-123",
                  "email": "ana@vet.com",
                  "senha": "123456"
                }
                """;

        //act + assert: so o admin cria usuarios
        mockMvc.perform(post("/veterinario")
                        .with(TokenTeste.usuario(Role.VETERINARIO, 3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());

        verify(cadastroVeterinarioService, never()).salvar(any());
    }
}
