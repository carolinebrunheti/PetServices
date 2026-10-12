package br.com.petServices.services;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.petServices.dtos.CadastroAdminDTO;
import br.com.petServices.dtos.CadastroAdminResponse;
import br.com.petServices.entities.CadastroAdminEntity;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroAdminRepository;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;


@Service 
public class CadastroAdminService {

    private final CadastroAdminRepository cadastroAdminRepository;

    private final UsuarioEventProducer usuarioEventProducer;

    

   public CadastroAdminService(CadastroAdminRepository cadastroAdminRepository,
        UsuarioEventProducer usuarioEventProducer) {
    this.cadastroAdminRepository = cadastroAdminRepository;
    this.usuarioEventProducer = usuarioEventProducer;
}

        public List<CadastroAdminResponse> buscarTodos(){
        return cadastroAdminRepository.findAll().stream()
        .map(e -> new CadastroAdminResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole())).toList();
    }

        public CadastroAdminResponse buscarPorId(Integer id){
          var admin =  cadastroAdminRepository.findById(id)
             .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
             return new CadastroAdminResponse(admin.getId(), admin.getNome(), admin.getEmail(), admin.getRole());
        }

    public CadastroAdminResponse salvar(CadastroAdminDTO request){
        CadastroAdminEntity admin = new CadastroAdminEntity();
        admin.setNome(request.nome());
        admin.setEmail(request.email());
        admin.setSenha(request.senha());
        admin.setRole(Role.VETERINARIO);


        CadastroAdminEntity salvo = cadastroAdminRepository.save(admin);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO,
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()
        ));
        return new CadastroAdminResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    public void removerAdmin(Integer id){
        CadastroAdminEntity admin = cadastroAdminRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Admin nao foi encontrado"));
        cadastroAdminRepository.delete(admin);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.REMOVIDO, admin.getId(), admin.getNome(), admin.getEmail(), admin.getRole()));
    }

    public CadastroAdminResponse atualizarAdmin(Integer id, CadastroAdminDTO request){
        CadastroAdminEntity admin = cadastroAdminRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Admin nao encontrado"));
        admin.setNome(request.nome());
        admin.setEmail(request.email());
        admin.setSenha(request.senha());
        CadastroAdminEntity salvo = cadastroAdminRepository.save(admin);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO, salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

        return new CadastroAdminResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }
}
