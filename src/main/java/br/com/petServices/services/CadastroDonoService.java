package br.com.petServices.services;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.petServices.dtos.CadastroDonoDTO;
import br.com.petServices.dtos.CadastroDonoResponse;
import br.com.petServices.entities.CadastroDonoEntity;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;

@Service 
public class CadastroDonoService {

    private final CadastroDonoRepository cadastroDonoRepository;
    private final UsuarioEventProducer usuarioEventProducer;


    
    public CadastroDonoService(CadastroDonoRepository cadastroDonoRepository,
            UsuarioEventProducer usuarioEventProducer) {
        this.cadastroDonoRepository = cadastroDonoRepository;
        this.usuarioEventProducer = usuarioEventProducer;
    }

    public List<CadastroDonoResponse> buscarTodos(){
        return cadastroDonoRepository.findAll().stream()
        .map(e -> new CadastroDonoResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole())).toList();
    }

    public CadastroDonoResponse buscarPorId(Integer id){
        var dono = cadastroDonoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dono não encontrado com o ID: " + id));
        return new CadastroDonoResponse(dono.getId(), dono.getNome(), dono.getEmail(), dono.getRole());
    }

    //kafka producer

    public CadastroDonoResponse cadastrar(CadastroDonoDTO request){
        CadastroDonoEntity dono = new CadastroDonoEntity();
        dono.setNome(request.nome());
        dono.setEmail(request.email());
        dono.setSenha(request.senha());
        dono.setRole(Role.TUTOR);

        CadastroDonoEntity salvo = cadastroDonoRepository.save(dono);
       usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO,
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()
        ));
        return new CadastroDonoResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    //atualizar

    public CadastroDonoResponse atualizar(CadastroDonoDTO request, Integer id){
        CadastroDonoEntity dono = cadastroDonoRepository.findById(id)
        .orElseThrow(()-> new RuntimeException("Dono nao encontrado"));
        dono.setNome(request.nome());
        dono.setEmail(request.email());

        CadastroDonoEntity salvo = cadastroDonoRepository.save(dono);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO,
            salvo.getId(), salvo.getNome(),salvo.getEmail(), salvo.getRole()
        ));
         return new CadastroDonoResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }
}
