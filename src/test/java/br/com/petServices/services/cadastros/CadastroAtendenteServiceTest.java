package br.com.petServices.services.cadastros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.petServices.dtos.CadastroAtendenteDTO;
import br.com.petServices.dtos.CadastroAtendenteResponse;
import br.com.petServices.entities.CadastroAtendenteEntity;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroAtendenteRepository;
import br.com.petServices.services.CadastroAtendenteService;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;

@ExtendWith(MockitoExtension.class)
class CadastroAtendenteServiceTest {

    @Mock
    private CadastroAtendenteRepository cadastroAtendenteRepository;

    @Mock
    private UsuarioEventProducer usuarioEventProducer;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CadastroAtendenteService cadastroAtendenteService;

    private CadastroAtendenteDTO novoAtendente() {
        return new CadastroAtendenteDTO("Carla", "carla@pet.com", "123456");
    }

    private CadastroAtendenteEntity atendenteSalvo() {
        return new CadastroAtendenteEntity(1, "Carla", "carla@pet.com", "123456", Role.ATENDENTE);
    }

    @Test
    @DisplayName("salvar: deve cadastrar o atendente com role ATENDENTE e publicar evento CRIADO")
    void salvarComSucesso() {
        // arrange
        when(passwordEncoder.encode("123456")).thenReturn("hash-bcrypt");
        when(cadastroAtendenteRepository.save(any(CadastroAtendenteEntity.class))).thenReturn(atendenteSalvo());

        // act
        CadastroAtendenteResponse resposta = cadastroAtendenteService.salvar(novoAtendente());

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Carla", resposta.nome());
        assertEquals(Role.ATENDENTE, resposta.role());

        ArgumentCaptor<CadastroAtendenteEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroAtendenteEntity.class);
        verify(cadastroAtendenteRepository).save(entidadeCaptor.capture());
        assertEquals(Role.ATENDENTE, entidadeCaptor.getValue().getRole());
        assertEquals("carla@pet.com", entidadeCaptor.getValue().getEmail());
        // a senha vai para o banco ja criptografada, nunca em texto puro
        assertEquals("hash-bcrypt", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.CRIADO, eventoCaptor.getValue().tipoEvento());
        assertEquals(1, eventoCaptor.getValue().id());
    }

    @Test
    @DisplayName("buscarTodos: deve retornar a lista de atendentes")
    void buscarTodos() {
        // arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(cadastroAtendenteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(atendenteSalvo())));

        // act
        Page<CadastroAtendenteResponse> resposta = cadastroAtendenteService.buscarTodos(pageable);

        // assert
        assertEquals(1, resposta.getContent().size());
        assertEquals("carla@pet.com", resposta.getContent().get(0).email());
    }

    @Test
    @DisplayName("buscarPorId: deve retornar o atendente quando existe")
    void buscarPorIdExistente() {
        // arrange
        when(cadastroAtendenteRepository.findById(1)).thenReturn(Optional.of(atendenteSalvo()));

        // act
        CadastroAtendenteResponse resposta = cadastroAtendenteService.buscarPorId(1);

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Carla", resposta.nome());
    }

    @Test
    @DisplayName("buscarPorId: deve lancar RecursoNaoEncontradoException quando nao existe")
    void buscarPorIdInexistente() {
        // arrange
        when(cadastroAtendenteRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroAtendenteService.buscarPorId(99));

        assertEquals("Atendente não encontrado com o ID: 99", erro.getMessage());
    }

    @Test
    @DisplayName("atualizarAtendente: deve atualizar os dados e publicar evento ATUALIZADO")
    void atualizarComSucesso() {
        // arrange
        CadastroAtendenteDTO alteracao = new CadastroAtendenteDTO("Carla Dias", "carla.dias@pet.com", "654321");
        when(cadastroAtendenteRepository.findById(1)).thenReturn(Optional.of(atendenteSalvo()));
        when(passwordEncoder.encode("654321")).thenReturn("hash-nova-senha");
        when(cadastroAtendenteRepository.save(any(CadastroAtendenteEntity.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        // act
        CadastroAtendenteResponse resposta = cadastroAtendenteService.atualizarAtendente(1, alteracao);

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Carla Dias", resposta.nome());
        assertEquals("carla.dias@pet.com", resposta.email());
        assertEquals(Role.ATENDENTE, resposta.role());

        ArgumentCaptor<CadastroAtendenteEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroAtendenteEntity.class);
        verify(cadastroAtendenteRepository).save(entidadeCaptor.capture());
        assertEquals("hash-nova-senha", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.ATUALIZADO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("atualizarAtendente: deve lancar excecao e nao salvar quando nao existe")
    void atualizarInexistente() {
        // arrange
        when(cadastroAtendenteRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroAtendenteService.atualizarAtendente(99, novoAtendente()));

        verify(cadastroAtendenteRepository, never()).save(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("removerAtendente: deve remover e publicar evento REMOVIDO")
    void removerComSucesso() {
        // arrange
        CadastroAtendenteEntity atendente = atendenteSalvo();
        when(cadastroAtendenteRepository.findById(1)).thenReturn(Optional.of(atendente));

        // act
        cadastroAtendenteService.removerAtendente(1);

        // assert
        verify(cadastroAtendenteRepository).delete(atendente);

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.REMOVIDO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("removerAtendente: deve lancar excecao e nao remover quando nao existe")
    void removerInexistente() {
        // arrange
        when(cadastroAtendenteRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroAtendenteService.removerAtendente(99));

        verify(cadastroAtendenteRepository, never()).delete(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }
}
