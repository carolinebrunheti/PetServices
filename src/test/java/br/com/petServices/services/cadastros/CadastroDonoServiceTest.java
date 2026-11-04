package br.com.petServices.services.cadastros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;
import br.com.petServices.dtos.CadastroDonoDTO;
import br.com.petServices.dtos.CadastroDonoResponse;
import br.com.petServices.entities.CadastroDonoEntity;
import br.com.petServices.exceptions.ConflitoException;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.services.CadastroDonoService;

@ExtendWith(MockitoExtension.class)
class CadastroDonoServiceTest {

    @Mock
    private CadastroDonoRepository cadastroDonoRepository;

    @Mock
    private UsuarioEventProducer usuarioEventProducer;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CadastroDonoService cadastroDonoService;

    private CadastroDonoDTO novoDono(){
        return new CadastroDonoDTO("Carole", "xuxubeleza@hotmail.com", "1234");
    }

    private CadastroDonoEntity donoSalvo(){
        return new CadastroDonoEntity(1, new ArrayList<>(), "Carole", "xuxubeleza@hotmail.com", "1234", Role.TUTOR);
    }

    @Test
    void cadastrarNovo(){
        //arrange(preparar)
        when(cadastroDonoRepository.existsByEmail("xuxubeleza@hotmail.com")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("hash-bcrypt");
        when(cadastroDonoRepository.save(any(CadastroDonoEntity.class))).thenReturn(donoSalvo());

        //act(agir)
        CadastroDonoResponse resposta = cadastroDonoService.cadastrar(novoDono());

        //assert(afirmar)
        assertEquals(1, resposta.id());
        assertEquals("Carole", resposta.nome());
        assertEquals(Role.TUTOR, resposta.role());

        ArgumentCaptor<CadastroDonoEntity> entidadeCaptor = ArgumentCaptor.forClass(CadastroDonoEntity.class);
        verify(cadastroDonoRepository).save(entidadeCaptor.capture());
        assertEquals(Role.TUTOR, entidadeCaptor.getValue().getRole());
        // a senha vai para o banco ja criptografada, nunca em texto puro
        assertEquals("hash-bcrypt", entidadeCaptor.getValue().getSenha());

        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.CRIADO, eventoCaptor.getValue().tipoEvento());
        assertEquals(1, eventoCaptor.getValue().id());
    }

    @Test
    void naoDeveCadastrarComEmailDuplicado(){
        //arrange(preparar)
        when(cadastroDonoRepository.existsByEmail("xuxubeleza@hotmail.com")).thenReturn(true);

        //act + assert
        ConflitoException erro = assertThrows(ConflitoException.class,
                () -> cadastroDonoService.cadastrar(novoDono()));

        assertEquals("Email ja cadastrado", erro.getMessage());
        verify(cadastroDonoRepository, never()).save(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }

    @Test 
    void atualizarDono(){
        CadastroDonoDTO alterar = new CadastroDonoDTO("Joao", "joao@hotmail.com", "1234");
        when(cadastroDonoRepository.findById(1)).thenReturn(Optional.of(donoSalvo()));
        when(cadastroDonoRepository.save(any(CadastroDonoEntity.class)))
        .thenAnswer(invocacao -> invocacao.getArgument(0));

        CadastroDonoResponse response = cadastroDonoService.atualizar( alterar, 1);

        assertEquals(1, response.id());
        assertEquals("Joao", response.nome());
        assertEquals("joao@hotmail.com", response.email());
        assertEquals(Role.TUTOR, response.role());
        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        assertEquals(TipoEvento.ATUALIZADO, eventoCaptor.getValue().tipoEvento());
    }

    @Test
    void removerDono(){
        //arrange
        // cria a entidade que finge estar no banco e guarda numa variavel,
        // porque o verify la embaixo precisa comparar com ESTE mesmo objeto
        CadastroDonoEntity remover = donoSalvo();
        // ensina o mock: quando o service chamar findById(1), devolve o "remover"
        // (Optional.of porque o findById do JpaRepository retorna Optional)
        when(cadastroDonoRepository.findById(1)).thenReturn(Optional.of(remover));

        //act
        // chama o service de verdade; por dentro ele faz findById(1) -> recebe o "remover"
        // -> delete(remover) -> publicar(evento REMOVIDO). O mock anota cada chamada
        cadastroDonoService.removerDono(1);

        //assert
        // confere que o delete foi chamado 1 vez e com o MESMO objeto que o findById devolveu
        verify(cadastroDonoRepository).delete(remover);
        // cria uma "armadilha" para capturar o evento que o service mandou para o producer
        ArgumentCaptor<UsuarioEvent> eventoCaptor = ArgumentCaptor.forClass(UsuarioEvent.class);
        // confere que publicar foi chamado 1 vez e captura o evento que foi passado
        verify(usuarioEventProducer).publicar(eventoCaptor.capture());
        // pega o evento capturado e confere que o tipo e REMOVIDO
        assertEquals(TipoEvento.REMOVIDO, eventoCaptor.getValue().tipoEvento());
    }

    @Test 
    void buscarLista(){

        Pageable pageable = PageRequest.of(0, 10);

        when(cadastroDonoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(donoSalvo())));

        Page<CadastroDonoResponse> resposta = cadastroDonoService.buscarTodos(pageable);

        assertEquals(1, resposta.getContent().size());
        assertEquals("xuxubeleza@hotmail.com", resposta.getContent().get(0).email());

    }

    @Test
    void buscarPorIdExistente(){
        //arrange
        when(cadastroDonoRepository.findById(1)).thenReturn(Optional.of(donoSalvo()));

        //act
        CadastroDonoResponse resposta = cadastroDonoService.buscarPorId(1);

        //assert
        assertEquals(1, resposta.id());
        assertEquals("Carole", resposta.nome());
        assertEquals(Role.TUTOR, resposta.role());
    }

    @Test
    void buscarPorIdInexistente(){
        //arrange
        when(cadastroDonoRepository.findById(99)).thenReturn(Optional.empty());

        //act + assert
        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroDonoService.buscarPorId(99));

        assertEquals("Dono não encontrado com o ID: 99", erro.getMessage());
    }

    @Test
    void naoDeveAtualizarDonoInexistente(){
        //arrange
        when(cadastroDonoRepository.findById(99)).thenReturn(Optional.empty());

        //act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroDonoService.atualizar(novoDono(), 99));

        verify(cadastroDonoRepository, never()).save(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }

    @Test
    void naoDeveRemoverDonoInexistente(){
        //arrange
        when(cadastroDonoRepository.findById(99)).thenReturn(Optional.empty());

        //act + assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> cadastroDonoService.removerDono(99));

        verify(cadastroDonoRepository, never()).delete(any());
        verify(usuarioEventProducer, never()).publicar(any());
    }
}