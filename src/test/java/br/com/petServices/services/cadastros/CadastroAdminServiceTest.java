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

import br.com.petServices.dtos.CadastroAdminDTO;
import br.com.petServices.dtos.CadastroAdminResponse;
import br.com.petServices.entities.CadastroAdminEntity;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroAdminRepository;
import br.com.petServices.services.CadastroAdminService;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;

@ExtendWith(MockitoExtension.class)
class CadastroAdminServiceTest {

    @Mock
    private CadastroAdminRepository cadastroAdminRepository;

    @Mock
    private UsuarioEventProducer usuarioEventProducer;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CadastroAdminService cadastroAdminService;

    private CadastroAdminDTO novoAdmin() {
        return new CadastroAdminDTO("Bia", "bia@admin.com", "123456");
    }

    private CadastroAdminEntity adminSalvo() {
        return new CadastroAdminEntity(1, "Bia", "bia@admin.com", "123456", Role.ADMIN);
    }

    @Test
    @DisplayName("salvar: deve cadastrar o admin com role ADMIN e publicar evento CRIADO")
    void salvarComSucesso() {
        // arrange
        when(passwordEncoder.encode("123456")).thenReturn("hash-bcrypt");
        when(cadastroAdminRepository.save(any(CadastroAdminEntity.class))).thenReturn(adminSalvo());

        // act
        CadastroAdminResponse resposta = cadastroAdminService.salvar(novoAdmin());

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Bia", resposta.nome());
        assertEquals(Role.ADMIN, resposta.role());

        ArgumentCaptor<CadastroAdminEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroAdminEntity.class);
        verify(cadastroAdminRepository).save(entidadeCaptor.capture());
        assertEquals(Role.ADMIN, entidadeCaptor.getValue().getRole());
        assertEquals("bia@admin.com", entidadeCaptor.getValue().getEmail());
        // a senha vai para o banco ja criptografada, nunca em texto puro
        assertEquals("hash-bcrypt", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.CRIADO, eventoCaptor.getValue().tipoEvento());
        assertEquals(1, eventoCaptor.getValue().id());
    }

    @Test
    @DisplayName("buscarTodos: deve retornar a lista de admins")
    void buscarTodos() {
        // arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(cadastroAdminRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(adminSalvo())));

        // act
        Page<CadastroAdminResponse> resposta = cadastroAdminService.buscarTodos(pageable);

        // assert
        assertEquals(1, resposta.getContent().size());
        assertEquals("bia@admin.com", resposta.getContent().get(0).email());
    }

    @Test
    @DisplayName("buscarPorId: deve retornar o admin quando existe")
    void buscarPorIdExistente() {
        // arrange
        when(cadastroAdminRepository.findById(1)).thenReturn(Optional.of(adminSalvo()));

        // act
        CadastroAdminResponse resposta = cadastroAdminService.buscarPorId(1);

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Bia", resposta.nome());
    }

    @Test
    @DisplayName("buscarPorId: deve lancar RecursoNaoEncontradoException quando nao existe")
    void buscarPorIdInexistente() {
        // arrange
        when(cadastroAdminRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroAdminService.buscarPorId(99));
    }

    @Test
    @DisplayName("atualizarAdmin: deve atualizar os dados e publicar evento ATUALIZADO")
    void atualizarComSucesso() {
        // arrange
        CadastroAdminDTO alteracao = new CadastroAdminDTO("Bia Lima", "bia.lima@admin.com", "654321");
        when(cadastroAdminRepository.findById(1)).thenReturn(Optional.of(adminSalvo()));
        when(passwordEncoder.encode("654321")).thenReturn("hash-nova-senha");
        when(cadastroAdminRepository.save(any(CadastroAdminEntity.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        // act
        CadastroAdminResponse resposta = cadastroAdminService.atualizarAdmin(1, alteracao);

        // assert
        assertEquals(1, resposta.id());
        assertEquals("Bia Lima", resposta.nome());
        assertEquals("bia.lima@admin.com", resposta.email());
        assertEquals(Role.ADMIN, resposta.role());

        ArgumentCaptor<CadastroAdminEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroAdminEntity.class);
        verify(cadastroAdminRepository).save(entidadeCaptor.capture());
        assertEquals("hash-nova-senha", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.ATUALIZADO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("atualizarAdmin: deve lancar excecao e nao salvar quando nao existe")
    void atualizarInexistente() {
        // arrange
        when(cadastroAdminRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroAdminService.atualizarAdmin(99, novoAdmin()));

        verify(cadastroAdminRepository, never()).save(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("removerAdmin: deve remover e publicar evento REMOVIDO")
    void removerComSucesso() {
        // arrange
        CadastroAdminEntity admin = adminSalvo();
        when(cadastroAdminRepository.findById(1)).thenReturn(Optional.of(admin));

        // act
        cadastroAdminService.removerAdmin(1);

        // assert
        verify(cadastroAdminRepository).delete(admin);

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.REMOVIDO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    @DisplayName("removerAdmin: deve lancar excecao e nao remover quando nao existe")
    void removerInexistente() {
        // arrange
        when(cadastroAdminRepository.findById(99)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroAdminService.removerAdmin(99));

        verify(cadastroAdminRepository, never()).delete(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }
}
