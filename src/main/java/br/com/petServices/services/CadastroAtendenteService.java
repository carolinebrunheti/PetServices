package br.com.petServices.services;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.dtos.CadastroAtendenteDTO;
import br.com.petServices.dtos.CadastroAtendenteResponse;
import br.com.petServices.entities.CadastroAtendenteEntity;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroAtendenteRepository;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;
import br.com.petServices.services.enums.Role;


@Service 
public class CadastroAtendenteService {

    private final CadastroAtendenteRepository cadastroAtendenteRepository;

    private final UsuarioEventProducer usuarioEventProducer;

    private final PasswordEncoder passwordEncoder;


    public CadastroAtendenteService(CadastroAtendenteRepository cadastroAtendenteRepository,
            UsuarioEventProducer usuarioEventProducer, PasswordEncoder passwordEncoder) {
        this.cadastroAtendenteRepository = cadastroAtendenteRepository;
        this.usuarioEventProducer = usuarioEventProducer;
        this.passwordEncoder = passwordEncoder;
    }

    public List<CadastroAtendenteResponse> buscarTodos(){
        return cadastroAtendenteRepository.findAll().stream()
        .map(e -> new CadastroAtendenteResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole())).toList();
    }

    public CadastroAtendenteResponse buscarPorId(Integer id){
        var atendente = cadastroAtendenteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atendente não encontrado com o ID: " + id));
        return new CadastroAtendenteResponse(atendente.getId(), atendente.getNome(), atendente.getEmail(), atendente.getRole());
    }

    public CadastroAtendenteResponse salvar(CadastroAtendenteDTO request){
        CadastroAtendenteEntity atendente = new CadastroAtendenteEntity();
        atendente.setNome(request.nome());
        atendente.setEmail(request.email());
        atendente.setSenha(passwordEncoder.encode(request.senha()));
        atendente.setRole(Role.ATENDENTE);

        CadastroAtendenteEntity salvo = cadastroAtendenteRepository.save(atendente);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO, 
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

            return new CadastroAtendenteResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
}

        public void removerAtendente(Integer id){
            CadastroAtendenteEntity atendente = cadastroAtendenteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atende nao encontrado"));
            cadastroAtendenteRepository.delete(atendente);
            usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.REMOVIDO, atendente.getId(), atendente.getNome(), atendente.getEmail(), atendente.getRole()));
        }
        
        public CadastroAtendenteResponse atualizarAtendente(Integer id, CadastroAtendenteDTO request){
            CadastroAtendenteEntity atendente  = cadastroAtendenteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atendente nao encontrada"));
            atendente.setNome(request.nome());
            atendente.setEmail(request.email());
            atendente.setSenha(passwordEncoder.encode(request.senha()));
            
            CadastroAtendenteEntity salvo = cadastroAtendenteRepository.save(atendente);
            usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO, salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

            return new CadastroAtendenteResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
        }
}