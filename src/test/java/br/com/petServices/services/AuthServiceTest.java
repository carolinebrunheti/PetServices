package br.com.petServices.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.petServices.dtos.LoginDTO;
import br.com.petServices.dtos.TokenResponse;
import br.com.petServices.entities.CadastroAdminEntity;
import br.com.petServices.entities.CadastroDonoEntity;
import br.com.petServices.exceptions.MuitasTentativasException;
import br.com.petServices.repositories.CadastroAdminRepository;
import br.com.petServices.repositories.CadastroAtendenteRepository;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.repositories.CadastroVeterinarioRepository;
import br.com.petServices.security.LimiteTentativasLogin;
import br.com.petServices.security.TokenService;
import br.com.petServices.security.UsuarioAutenticado;
import br.com.petServices.services.enums.Role;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CadastroAdminRepository cadastroAdminRepository;

    @Mock
    private CadastroAtendenteRepository cadastroAtendenteRepository;

    @Mock
    private CadastroVeterinarioRepository cadastroVeterinarioRepository;

    @Mock
    private CadastroDonoRepository cadastroDonoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private LimiteTentativasLogin limiteTentativasLogin;

    @InjectMocks
    private AuthService authService;

    private static final String IP = "10.0.0.1";

    private TokenResponse tokenGerado() {
        return new TokenResponse("token-falso", "Bearer", Instant.now());
    }

    @Test
    @DisplayName("login: admin com senha certa recebe token com role ADMIN")
    void loginAdminComSucesso() {
        // arrange
        CadastroAdminEntity admin = new CadastroAdminEntity(1, "Admin", "admin@petservices.com", "hash", Role.ADMIN);
        when(cadastroAdminRepository.findByEmail("admin@petservices.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        TokenResponse token = tokenGerado();
        when(tokenService.gerar(any(UsuarioAutenticado.class))).thenReturn(token);

        // act
        TokenResponse resposta = authService.login(new LoginDTO("admin@petservices.com", "123456"), IP);

        // assert
        assertSame(token, resposta);
        ArgumentCaptor<UsuarioAutenticado> usuarioCaptor = ArgumentCaptor.forClass(UsuarioAutenticado.class);
        verify(tokenService).gerar(usuarioCaptor.capture());
        assertEquals(1, usuarioCaptor.getValue().id());
        assertEquals(Role.ADMIN, usuarioCaptor.getValue().role());
    }

    @Test
    @DisplayName("login: quando o email nao e de admin/atendente/vet, procura nos donos")
    void loginDonoComSucesso() {
        // arrange: os outros repositories devolvem Optional.empty() por padrao no mock
        CadastroDonoEntity dono = new CadastroDonoEntity(5, new ArrayList<>(), "Carlos", "carlos@email.com", "hash", Role.TUTOR);
        when(cadastroDonoRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(tokenService.gerar(any(UsuarioAutenticado.class))).thenReturn(tokenGerado());

        // act
        authService.login(new LoginDTO("carlos@email.com", "123456"), IP);

        // assert
        ArgumentCaptor<UsuarioAutenticado> usuarioCaptor = ArgumentCaptor.forClass(UsuarioAutenticado.class);
        verify(tokenService).gerar(usuarioCaptor.capture());
        assertEquals(5, usuarioCaptor.getValue().id());
        assertEquals(Role.TUTOR, usuarioCaptor.getValue().role());
    }

    @Test
    @DisplayName("login: senha errada deve lancar BadCredentialsException e nao gerar token")
    void loginComSenhaErrada() {
        // arrange
        CadastroAdminEntity admin = new CadastroAdminEntity(1, "Admin", "admin@petservices.com", "hash", Role.ADMIN);
        when(cadastroAdminRepository.findByEmail("admin@petservices.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha-errada", "hash")).thenReturn(false);

        // act + assert
        BadCredentialsException erro = assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginDTO("admin@petservices.com", "senha-errada"), IP));

        assertEquals("Email ou senha inválidos", erro.getMessage());
        verify(tokenService, never()).gerar(any());
    }

    @Test
    @DisplayName("login: email que nao existe em nenhuma tabela deve lancar BadCredentialsException")
    void loginComEmailInexistente() {
        // act + assert
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginDTO("ninguem@email.com", "123456"), IP));

        verify(tokenService, never()).gerar(any());
    }

    @Test
    @DisplayName("login: email inexistente ainda compara a senha, para o tempo de resposta nao revelar o email")
    void loginComEmailInexistenteAindaRodaOBcrypt() {
        // act
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginDTO("ninguem@email.com", "123456"), IP));

        // assert
        verify(passwordEncoder).matches(eq("123456"), any());
        verify(limiteTentativasLogin).registrarFalha(IP);
    }

    @Test
    @DisplayName("login: senha errada conta como falha para o IP")
    void loginComSenhaErradaRegistraFalha() {
        // arrange
        CadastroAdminEntity admin = new CadastroAdminEntity(1, "Admin", "admin@petservices.com", "hash", Role.ADMIN);
        when(cadastroAdminRepository.findByEmail("admin@petservices.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha-errada", "hash")).thenReturn(false);

        // act
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginDTO("admin@petservices.com", "senha-errada"), IP));

        // assert
        verify(limiteTentativasLogin).registrarFalha(IP);
        verify(limiteTentativasLogin, never()).registrarSucesso(any());
    }

    @Test
    @DisplayName("login: com sucesso zera as falhas do IP")
    void loginComSucessoZeraAsFalhas() {
        // arrange
        CadastroAdminEntity admin = new CadastroAdminEntity(1, "Admin", "admin@petservices.com", "hash", Role.ADMIN);
        when(cadastroAdminRepository.findByEmail("admin@petservices.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(tokenService.gerar(any(UsuarioAutenticado.class))).thenReturn(tokenGerado());

        // act
        authService.login(new LoginDTO("admin@petservices.com", "123456"), IP);

        // assert
        verify(limiteTentativasLogin).registrarSucesso(IP);
        verify(limiteTentativasLogin, never()).registrarFalha(any());
    }

    @Test
    @DisplayName("login: IP bloqueado nem chega a consultar o banco")
    void loginComIpBloqueado() {
        // arrange
        doThrow(new MuitasTentativasException("Muitas tentativas")).when(limiteTentativasLogin).verificar(IP);

        // act + assert
        assertThrows(MuitasTentativasException.class,
                () -> authService.login(new LoginDTO("admin@petservices.com", "123456"), IP));

        verify(cadastroAdminRepository, never()).findByEmail(any());
        verify(tokenService, never()).gerar(any());
    }
}
