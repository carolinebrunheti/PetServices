package br.com.petServices.services.cadastros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.petServices.dtos.CadastroPetDTO;
import br.com.petServices.dtos.CadastroPetResponse;
import br.com.petServices.entities.CadastroDonoEntity;
import br.com.petServices.entities.CadastroPetEntity;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.repositories.CadastroPetRepository;
import br.com.petServices.services.CadastroPetService;
import br.com.petServices.services.enums.Especie;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.PetEvent;
import br.com.petServices.services.enums.evento.PetEventProducer;
import br.com.petServices.services.enums.evento.TipoEvento;

@ExtendWith(MockitoExtension.class)
class CadastroPetServiceTest {

    @Mock
    private CadastroPetRepository cadastroPetRepository;

    @Mock
    private CadastroDonoRepository cadastroDonoRepository;

    @Mock
    private PetEventProducer petEventProducer;

    @InjectMocks
    private CadastroPetService cadastroPetService;

    private static final LocalDate NASCIMENTO = LocalDate.of(2022, 5, 10);

    private CadastroDonoEntity dono() {
        return new CadastroDonoEntity(1, new ArrayList<>(), "Carole", "carole@exemplo.com", "123456", Role.TUTOR);
    }

    private CadastroPetDTO novoPet() {
        return new CadastroPetDTO("Rex", NASCIMENTO, "Vira-lata", Especie.CACHORRO, 1);
    }

    private CadastroPetEntity petSalvo() {
        return new CadastroPetEntity(10, dono(), "Rex", NASCIMENTO, "Vira-lata", Especie.CACHORRO);
    }

    @Test
    @DisplayName("salvarPet: deve cadastrar o pet vinculado ao dono e publicar evento CRIADO")
    void salvarComSucesso() {
        // arrange
        CadastroDonoEntity dono = dono();
        when(cadastroDonoRepository.findById(1)).thenReturn(Optional.of(dono));
        when(cadastroPetRepository.save(any(CadastroPetEntity.class))).thenReturn(petSalvo());

        // act
        CadastroPetResponse resposta = cadastroPetService.salvarPet(novoPet());

        // assert
        assertEquals(10, resposta.id());
        assertEquals("Rex", resposta.nome());
        assertEquals(Especie.CACHORRO, resposta.especie());
        assertEquals(1, resposta.donoId());

        // o pet que foi para o save precisa estar ligado ao dono que veio do findById
        ArgumentCaptor<CadastroPetEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroPetEntity.class);
        verify(cadastroPetRepository).save(entidadeCaptor.capture());
        assertSame(dono, entidadeCaptor.getValue().getDono());
        assertEquals(NASCIMENTO, entidadeCaptor.getValue().getDataNascimento());

        ArgumentCaptor<PetEvent> eventoCaptor = ArgumentCaptor.forClass(PetEvent.class);
        verify(petEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.CRIADO, eventoCaptor.getValue().tipoEvento());
        assertEquals(10, eventoCaptor.getValue().id());
        assertEquals(1, eventoCaptor.getValue().donoId());
    }

    @Test
    @DisplayName("salvarPet: deve lancar excecao e nao salvar quando o dono nao existe")
    void salvarComDonoInexistente() {
        // arrange
        when(cadastroDonoRepository.findById(1)).thenReturn(Optional.empty());

        // act + assert
        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroPetService.salvarPet(novoPet()));

        assertEquals("Dono não encontrado com o ID: 1", erro.getMessage());
        verify(cadastroPetRepository, never()).save(any());
        verify(petEventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("buscarTodos: deve retornar a lista de pets com o id do dono")
    void buscarTodos() {
        // arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(cadastroPetRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(petSalvo())));

        // act
        Page<CadastroPetResponse> resposta = cadastroPetService.buscarTodos(pageable);

        // assert
        assertEquals(1, resposta.getContent().size());
        assertEquals("Rex", resposta.getContent().get(0).nome());
        assertEquals(1, resposta.getContent().get(0).donoId());
    }

    @Test
    @DisplayName("buscarPorId: deve retornar o pet quando existe")
    void buscarPorIdExistente() {
        // arrange
        when(cadastroPetRepository.findById(10)).thenReturn(Optional.of(petSalvo()));

        // act
        CadastroPetResponse resposta = cadastroPetService.buscarPorId(10);

        // assert
        assertEquals(10, resposta.id());
        assertEquals("Vira-lata", resposta.raca());
        assertEquals(NASCIMENTO, resposta.dataNascimento());
    }

    @Test
    @DisplayName("buscarPorId: deve lancar RecursoNaoEncontradoException quando nao existe")
    void buscarPorIdInexistente() {
        // arrange
        when(cadastroPetRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroPetService.buscarPorId(99));

        assertEquals("Pet não encontrado com o ID: 99", erro.getMessage());
    }

    @Test
    @DisplayName("atualizarPet: deve atualizar os dados, manter o dono e publicar evento ATUALIZADO")
    void atualizarComSucesso() {
        // arrange
        LocalDate novaData = LocalDate.of(2021, 1, 15);
        CadastroPetDTO alteracao = new CadastroPetDTO("Mimi", novaData, "Siames", Especie.GATO, 1);
        when(cadastroPetRepository.findById(10)).thenReturn(Optional.of(petSalvo()));
        when(cadastroPetRepository.save(any(CadastroPetEntity.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        // act
        CadastroPetResponse resposta = cadastroPetService.atualizarPet(10, alteracao);

        // assert
        assertEquals(10, resposta.id());
        assertEquals("Mimi", resposta.nome());
        assertEquals("Siames", resposta.raca());
        assertEquals(Especie.GATO, resposta.especie());
        assertEquals(novaData, resposta.dataNascimento());
        assertEquals(1, resposta.donoId());

        ArgumentCaptor<PetEvent> eventoCaptor = ArgumentCaptor.forClass(PetEvent.class);
        verify(petEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.ATUALIZADO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("atualizarPet: deve lancar excecao e nao salvar quando nao existe")
    void atualizarInexistente() {
        // arrange
        when(cadastroPetRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroPetService.atualizarPet(99, novoPet()));

        verify(cadastroPetRepository, never()).save(any());
        verify(petEventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("deletarPet: deve remover e publicar evento REMOVIDO")
    void deletarComSucesso() {
        // arrange
        CadastroPetEntity pet = petSalvo();
        when(cadastroPetRepository.findById(10)).thenReturn(Optional.of(pet));

        // act
        cadastroPetService.deletarPet(10);

        // assert
        verify(cadastroPetRepository).delete(pet);

        ArgumentCaptor<PetEvent> eventoCaptor = ArgumentCaptor.forClass(PetEvent.class);
        verify(petEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.REMOVIDO, eventoCaptor.getValue().tipoEvento());
        assertEquals(1, eventoCaptor.getValue().donoId());
    }

    @Test
    @DisplayName("deletarPet: deve lancar excecao e nao remover quando nao existe")
    void deletarInexistente() {
        // arrange
        when(cadastroPetRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroPetService.deletarPet(99));

        verify(cadastroPetRepository, never()).delete(any());
        verify(petEventProducer, never()).publicar(any());
    }
}
