package br.com.petServices.controllers;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.petServices.config.SecurityConfig;
import br.com.petServices.dtos.CadastroDonoDTO;
import br.com.petServices.dtos.CadastroDonoResponse;
import br.com.petServices.security.TokenTeste;
import br.com.petServices.services.CadastroDonoService;
import br.com.petServices.services.enums.Role;


// o @Import traz as regras de seguranca de verdade para dentro do teste
@WebMvcTest(controllers = CadastroDonoController.class, properties = "jwt.secret=" + TokenTeste.SECRET)
@Import(SecurityConfig.class)
public class CadastroDonoControllerTest {
    
    @Autowired 
    private MockMvc mockMvc;

    @MockitoBean 
    private CadastroDonoService cadastroDonoService;


    @Test
    void TestarCriarUsuario201 () throws Exception{
        //arrange
        CadastroDonoResponse salvarUsuario = new CadastroDonoResponse(1, "Luan", "luan@hotmail.com", Role.TUTOR);
        when(cadastroDonoService.cadastrar(any(CadastroDonoDTO.class))).thenReturn(salvarUsuario);

        //corpo da requisicao postman
        String json = """
        {
        "nome": "Luan",
        "email": "luan@hotmail.com",
        "senha":"123456"}
        """;

        //act + assert

        mockMvc.perform(post("/dono")
        .with(TokenTeste.usuario(Role.ADMIN, 1))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nome").value("Luan"))
        .andExpect(jsonPath("$.email").value("luan@hotmail.com"))
        .andExpect(jsonPath("$.role").value("TUTOR"));

        verify(cadastroDonoService).cadastrar(any(CadastroDonoDTO.class));
    }

    private static final String NOVO_DONO = """
        {
        "nome": "Luan",
        "email": "luan@hotmail.com",
        "senha":"123456"}
        """;

    @Test
    void criarDonoSemTokenDeveRetornar401() throws Exception{
        //act + assert: sem header Authorization
        mockMvc.perform(post("/dono")
        .contentType(MediaType.APPLICATION_JSON)
        .content(NOVO_DONO))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

        verify(cadastroDonoService, never()).cadastrar(any());
    }

    @Test
    void atendenteNaoPodeCriarDonoDeveRetornar403() throws Exception{
        //act + assert: so o admin cria usuarios
        mockMvc.perform(post("/dono")
        .with(TokenTeste.usuario(Role.ATENDENTE, 2))
        .contentType(MediaType.APPLICATION_JSON)
        .content(NOVO_DONO))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));

        verify(cadastroDonoService, never()).cadastrar(any());
    }

    @Test
    void tutorNaoPodeRemoverDonoDeveRetornar403() throws Exception{
        //act + assert: so o admin remove usuarios
        mockMvc.perform(delete("/dono/1")
        .with(TokenTeste.usuario(Role.TUTOR, 1)))
        .andExpect(status().isForbidden());

        verify(cadastroDonoService, never()).removerDono(any());
    }

    @Test
    void adminPodeRemoverDonoDeveRetornar204() throws Exception{
        //act + assert
        mockMvc.perform(delete("/dono/1")
        .with(TokenTeste.usuario(Role.ADMIN, 1)))
        .andExpect(status().isNoContent());

        verify(cadastroDonoService).removerDono(1);
    }
    
}
