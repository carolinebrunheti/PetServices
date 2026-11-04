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

import br.com.petServices.dtos.CadastroVeterinarioDTO;
import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.entities.CadastroVeterinarioEntity;
import br.com.petServices.exceptions.ConflitoException;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroVeterinarioRepository;
import br.com.petServices.services.CadastroVeterinarioService;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;

@ExtendWith(MockitoExtension.class)
class CadastroVeterinarioServiceTest {

    @Mock
    private CadastroVeterinarioRepository cadastroVeterinarioRepository;

    @Mock
    private UsuarioEventProducer usuarioEventProducer;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CadastroVeterinarioService cadastroVeterinarioService;

    private CadastroVeterinarioDTO novoVeterinario() {
        return new CadastroVeterinarioDTO("Ana", "CRM-123", "Felinos", "ana@vet.com", "123456");
    }

    private CadastroVeterinarioEntity veterinarioSalvo() {
        return new CadastroVeterinarioEntity(1, "Ana", "CRM-123", "Felinos", "ana@vet.com", "123456", Role.VETERINARIO);
    }

    @Test
    @DisplayName("salvar: deve cadastrar o veterinario e publicar evento CRIADO")
    void salvarComSucesso() {
        // arrange
        when(cadastroVeterinarioRepository.existsByEmail("ana@vet.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash-bcrypt");
        when(cadastroVeterinarioRepository.save(any(CadastroVeterinarioEntity.class))).thenReturn(veterinarioSalvo());

        // act
        CadastroVeterinarioResponse resposta = cadastroVeterinarioService.salvar(novoVeterinario());

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Ana", resposta.nome());
        assertEquals(Role.VETERINARIO, resposta.role());

        ArgumentCaptor<CadastroVeterinarioEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroVeterinarioEntity.class);
        verify(cadastroVeterinarioRepository).save(entidadeCaptor.capture());
        assertEquals(Role.VETERINARIO, entidadeCaptor.getValue().getRole());
        assertEquals("CRM-123", entidadeCaptor.getValue().getCrm());
        assertEquals("hash-bcrypt", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.CRIADO, eventoCaptor.getValue().tipoEvento());
        assertEquals(1, eventoCaptor.getValue().id());
    }

    @Test
    @DisplayName("salvar: deve lancar ConflitoException quando o email ja existe")
    void salvarComEmailDuplicado() {
        // arrange
        when(cadastroVeterinarioRepository.existsByEmail("ana@vet.com")).thenReturn(true);

        // act + assert
        ConflitoException erro = assertThrows(ConflitoException.class,
                () -> cadastroVeterinarioService.salvar(novoVeterinario()));

        assertEquals("Email ja cadastrado", erro.getMessage());
        verify(cadastroVeterinarioRepository, never()).save(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("buscarTodos: deve retornar a lista de veterinarios")
    void buscarTodos() {
        // arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(cadastroVeterinarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(veterinarioSalvo())));

        // act
        Page<CadastroVeterinarioResponse> resposta = cadastroVeterinarioService.buscarTodos(pageable);

        // assert
        assertEquals(1, resposta.getContent().size());
        assertEquals("ana@vet.com", resposta.getContent().get(0).email());
    }

    @Test
    @DisplayName("buscarPorId: deve retornar o veterinario quando existe")
    void buscarPorIdExistente() {
        // arrange
        when(cadastroVeterinarioRepository.findById(1)).thenReturn(Optional.of(veterinarioSalvo()));

        // act
        CadastroVeterinarioResponse resposta = cadastroVeterinarioService.buscarPorId(1);

        // assert
        assertEquals(1, resposta.id());
        assertEquals("CRM-123", resposta.crm());
    }

    @Test
    @DisplayName("buscarPorId: deve lancar RecursoNaoEncontradoException quando nao existe")
    void buscarPorIdInexistente() {
        // arrange
        when(cadastroVeterinarioRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroVeterinarioService.buscarPorId(99));
    }

    @Test
    @DisplayName("atualizarVeterinario: deve atualizar os dados e publicar evento ATUALIZADO")
    void atualizarComSucesso() {
        // arrange
        CadastroVeterinarioDTO alteracao = new CadastroVeterinarioDTO("Ana Souza", "CRM-123", "Caninos", "ana@vet.com", "654321");
        when(cadastroVeterinarioRepository.findById(1)).thenReturn(Optional.of(veterinarioSalvo()));
        when(passwordEncoder.encode("654321")).thenReturn("hash-nova-senha");
        when(cadastroVeterinarioRepository.save(any(CadastroVeterinarioEntity.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        // act
        CadastroVeterinarioResponse resposta = cadastroVeterinarioService.atualizarVeterinario(1, alteracao);

        // assert
        assertEquals("Ana Souza", resposta.nome());
        assertEquals("Caninos", resposta.especialidade());

        ArgumentCaptor<CadastroVeterinarioEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroVeterinarioEntity.class);
        verify(cadastroVeterinarioRepository).save(entidadeCaptor.capture());
        assertEquals("hash-nova-senha", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.ATUALIZADO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("atualizarVeterinario: deve lancar excecao e nao salvar quando nao existe")
    void atualizarInexistente() {
        // arrange
        when(cadastroVeterinarioRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroVeterinarioService.atualizarVeterinario(99, novoVeterinario()));

        verify(cadastroVeterinarioRepository, never()).save(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("removerVeterinario: deve remover e publicar evento REMOVIDO")
    void removerComSucesso() {
        // arrange
        CadastroVeterinarioEntity veterinario = veterinarioSalvo();
        when(cadastroVeterinarioRepository.findById(1)).thenReturn(Optional.of(veterinario));

        // act
        cadastroVeterinarioService.removerVeterinario(1);

        // assert
        verify(cadastroVeterinarioRepository).delete(veterinario);

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.REMOVIDO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("removerVeterinario: deve lancar excecao e nao remover quando nao existe")
    void removerInexistente() {
        // arrange
        when(cadastroVeterinarioRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroVeterinarioService.removerVeterinario(99));

        verify(cadastroVeterinarioRepository, never()).delete(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }
}
