package br.com.petServices.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.petServices.dtos.CadastroDonoDTO;
import br.com.petServices.dtos.CadastroDonoResponse;
import br.com.petServices.entities.CadastroDonoEntity;
import br.com.petServices.exceptions.ConflitoException;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;

@Service 
public class CadastroDonoService {

    private final CadastroDonoRepository cadastroDonoRepository;
    private final UsuarioEventProducer usuarioEventProducer;
    private final PasswordEncoder passwordEncoder;


    
    public CadastroDonoService(CadastroDonoRepository cadastroDonoRepository,
            UsuarioEventProducer usuarioEventProducer, PasswordEncoder passwordEncoder) {
        this.cadastroDonoRepository = cadastroDonoRepository;
        this.usuarioEventProducer = usuarioEventProducer;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<CadastroDonoResponse> buscarTodos(Pageable pageable){
        return cadastroDonoRepository.findAll(pageable)
        .map(e -> new CadastroDonoResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole()));
    }

    public CadastroDonoResponse buscarPorId(Integer id){
        var dono = cadastroDonoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Dono não encontrado com o ID: " + id));
        return new CadastroDonoResponse(dono.getId(), dono.getNome(), dono.getEmail(), dono.getRole());
    }

    //kafka producer

    @Transactional

    public CadastroDonoResponse cadastrar(CadastroDonoDTO request){
        if (cadastroDonoRepository.existsByEmail(request.email())) {
            throw new ConflitoException("Email ja cadastrado");
        }
        CadastroDonoEntity dono = new CadastroDonoEntity();
        dono.setNome(request.nome());
        dono.setEmail(request.email());
        dono.setSenha(passwordEncoder.encode(request.senha()));
        dono.setRole(Role.TUTOR);

        CadastroDonoEntity salvo = cadastroDonoRepository.save(dono);
       usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO,
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()
        ));
        return new CadastroDonoResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    //atualizar

    @Transactional

    public CadastroDonoResponse atualizar(CadastroDonoDTO request, Integer id){
        CadastroDonoEntity dono = cadastroDonoRepository.findById(id)
        .orElseThrow(()-> new RecursoNaoEncontradoException("Dono nao encontrado"));
        dono.setNome(request.nome());
        dono.setEmail(request.email());

        CadastroDonoEntity salvo = cadastroDonoRepository.save(dono);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO,
            salvo.getId(), salvo.getNome(),salvo.getEmail(), salvo.getRole()
        ));
         return new CadastroDonoResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    //remover

    @Transactional

    public void removerDono(Integer id){
        CadastroDonoEntity dono = cadastroDonoRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Donoo nao encontrado"));
        cadastroDonoRepository.delete(dono);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.REMOVIDO,
            dono.getId(),dono.getNome(), dono.getEmail(), dono.getRole()
        ));
    }
}
