package br.com.petServices.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.petServices.config.SecurityConfig;
import br.com.petServices.dtos.CadastroPetResponse;
import br.com.petServices.entities.CadastroPetEntity;
import br.com.petServices.security.TokenTeste;
import br.com.petServices.services.CadastroPetService;
import br.com.petServices.services.enums.Especie;
import br.com.petServices.services.enums.Role;

@WebMvcTest(controllers = CadastroPetController.class, properties = "jwt.secret=" + TokenTeste.SECRET)
@Import(SecurityConfig.class)
class CadastroPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CadastroPetService cadastroPetService;

    // pet que pertence ao dono de id 5
    private CadastroPetResponse petDoDono5() {
        return new CadastroPetResponse(10, "Rex", LocalDate.of(2022, 5, 10), "Vira-lata", Especie.CACHORRO, 5);
    }

    @Test
    void tutorDeveVerSoOsPropriosPets() throws Exception {
        //arrange
        when(cadastroPetService.buscarPorDono(eq(5), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(petDoDono5())));

        //act + assert
        mockMvc.perform(get("/pet")
                        .with(TokenTeste.usuario(Role.TUTOR, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].donoId").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // o id do dono vem do token, e a lista completa nunca e consultada
        verify(cadastroPetService).buscarPorDono(eq(5), any());
        verify(cadastroPetService, never()).buscarTodos(any());
    }

    @Test
    void atendenteDeveVerTodosOsPets() throws Exception {
        //arrange
        when(cadastroPetService.buscarTodos(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(petDoDono5())));

        //act + assert
        mockMvc.perform(get("/pet")
                        .with(TokenTeste.usuario(Role.ATENDENTE, 2)))
                .andExpect(status().isOk());

        verify(cadastroPetService).buscarTodos(any());
        verify(cadastroPetService, never()).buscarPorDono(any(), any());
    }

    @Test
    void listagemDevePaginarCom10ItensOrdenadosPorIdPorPadrao() throws Exception {
        //arrange
        when(cadastroPetService.buscarTodos(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        //act
        mockMvc.perform(get("/pet")
                        .with(TokenTeste.usuario(Role.ATENDENTE, 2)))
                .andExpect(status().isOk());

        //assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(cadastroPetService).buscarTodos(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void listagemDeveRespeitarPaginaPedidaELimitarTamanhoEm50() throws Exception {
        //arrange
        when(cadastroPetService.buscarTodos(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        //act
        mockMvc.perform(get("/pet").param("page", "2").param("size", "1000")
                        .with(TokenTeste.usuario(Role.ATENDENTE, 2)))
                .andExpect(status().isOk());

        //assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(cadastroPetService).buscarTodos(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void listagemComCampoDeOrdenacaoInexistenteDeveRetornar400() throws Exception {
        //arrange
        when(cadastroPetService.buscarTodos(any(Pageable.class))).thenThrow(new PropertyReferenceException(
                "naoExiste", TypeInformation.of(CadastroPetEntity.class), List.of()));

        //act + assert
        mockMvc.perform(get("/pet").param("sort", "naoExiste")
                        .with(TokenTeste.usuario(Role.ATENDENTE, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Campo de ordenação inválido: naoExiste"));
    }

    @Test
    void tutorPodeVerOProprioPet() throws Exception {
        //arrange
        when(cadastroPetService.buscarPorId(10)).thenReturn(petDoDono5());

        //act + assert
        mockMvc.perform(get("/pet/10")
                        .with(TokenTeste.usuario(Role.TUTOR, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Rex"));
    }

    @Test
    void tutorNaoPodeVerPetDeOutroTutorDeveRetornar403() throws Exception {
        //arrange: o pet 10 e do dono 5, mas quem esta logado e o dono 8
        when(cadastroPetService.buscarPorId(10)).thenReturn(petDoDono5());

        //act + assert
        mockMvc.perform(get("/pet/10")
                        .with(TokenTeste.usuario(Role.TUTOR, 8)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tutorNaoPodeCadastrarPetDeveRetornar403() throws Exception {
        //arrange
        String json = """
                {
                  "nome": "Rex",
                  "dataNascimento": "2022-05-10",
                  "especie": "CACHORRO",
                  "donoId": 5
                }
                """;

        //act + assert: so admin e atendente cadastram pets
        mockMvc.perform(post("/pet")
                        .with(TokenTeste.usuario(Role.TUTOR, 5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());

        verify(cadastroPetService, never()).salvarPet(any());
    }

    @Test
    void atendentePodeRemoverPetDeveRetornar204() throws Exception {
        //act + assert
        mockMvc.perform(delete("/pet/10")
                        .with(TokenTeste.usuario(Role.ATENDENTE, 2)))
                .andExpect(status().isNoContent());

        verify(cadastroPetService).deletarPet(10);
    }
}
