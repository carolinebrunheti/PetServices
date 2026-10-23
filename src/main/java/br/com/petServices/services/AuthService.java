package br.com.petServices.services;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.petServices.dtos.LoginDTO;
import br.com.petServices.dtos.TokenResponse;
import br.com.petServices.repositories.CadastroAdminRepository;
import br.com.petServices.repositories.CadastroAtendenteRepository;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.repositories.CadastroVeterinarioRepository;
import br.com.petServices.security.LimiteTentativasLogin;
import br.com.petServices.security.TokenService;
import br.com.petServices.security.UsuarioAutenticado;

@Service
public class AuthService {

    private final CadastroAdminRepository cadastroAdminRepository;
    private final CadastroAtendenteRepository cadastroAtendenteRepository;
    private final CadastroVeterinarioRepository cadastroVeterinarioRepository;
    private final CadastroDonoRepository cadastroDonoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimiteTentativasLogin limiteTentativasLogin;

    // hash de uma senha qualquer, comparado quando o email nao existe
    private final String hashFicticio;

    public AuthService(CadastroAdminRepository cadastroAdminRepository,
            CadastroAtendenteRepository cadastroAtendenteRepository,
            CadastroVeterinarioRepository cadastroVeterinarioRepository,
            CadastroDonoRepository cadastroDonoRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            LimiteTentativasLogin limiteTentativasLogin) {
        this.cadastroAdminRepository = cadastroAdminRepository;
        this.cadastroAtendenteRepository = cadastroAtendenteRepository;
        this.cadastroVeterinarioRepository = cadastroVeterinarioRepository;
        this.cadastroDonoRepository = cadastroDonoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limiteTentativasLogin = limiteTentativasLogin;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-igualar-o-tempo");
    }

    public TokenResponse login(LoginDTO request, String ip) {
        limiteTentativasLogin.verificar(ip);

        Optional<UsuarioAutenticado> usuario = buscarPorEmail(request.email());

        // o BCrypt roda mesmo quando o email nao existe: assim o tempo de resposta
        // e o mesmo nos dois casos e nao denuncia quais emails estao cadastrados
        String hash = usuario.map(UsuarioAutenticado::senha).orElse(hashFicticio);
        boolean senhaConfere = passwordEncoder.matches(request.senha(), hash);

        if (usuario.isEmpty() || !senhaConfere) {
            limiteTentativasLogin.registrarFalha(ip);
            // mesma mensagem para email inexistente e senha errada
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        limiteTentativasLogin.registrarSucesso(ip);
        return tokenService.gerar(usuario.get());
    }

    // os usuarios ficam em 4 tabelas; procura nesta ordem e para no primeiro que achar
    private Optional<UsuarioAutenticado> buscarPorEmail(String email) {
        return cadastroAdminRepository.findByEmail(email)
                .map(a -> new UsuarioAutenticado(a.getId(), a.getEmail(), a.getSenha(), a.getRole()))
                .or(() -> cadastroAtendenteRepository.findByEmail(email)
                        .map(a -> new UsuarioAutenticado(a.getId(), a.getEmail(), a.getSenha(), a.getRole())))
                .or(() -> cadastroVeterinarioRepository.findByEmail(email)
                        .map(v -> new UsuarioAutenticado(v.getId(), v.getEmail(), v.getSenha(), v.getRole())))
                .or(() -> cadastroDonoRepository.findByEmail(email)
                        .map(d -> new UsuarioAutenticado(d.getId(), d.getEmail(), d.getSenha(), d.getRole())));
    }
}
