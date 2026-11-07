package br.com.petServices.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final PasswordEncoder passwordEncoder;

    

   public CadastroAdminService(CadastroAdminRepository cadastroAdminRepository,
        UsuarioEventProducer usuarioEventProducer, PasswordEncoder passwordEncoder) {
    this.cadastroAdminRepository = cadastroAdminRepository;
    this.usuarioEventProducer = usuarioEventProducer;
    this.passwordEncoder = passwordEncoder;
}

        public Page<CadastroAdminResponse> buscarTodos(Pageable pageable){
        return cadastroAdminRepository.findAll(pageable)
        .map(e -> new CadastroAdminResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole()));
    }

        public CadastroAdminResponse buscarPorId(Integer id){
          var admin =  cadastroAdminRepository.findById(id)
             .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
             return new CadastroAdminResponse(admin.getId(), admin.getNome(), admin.getEmail(), admin.getRole());
        }

    @Transactional

    public CadastroAdminResponse salvar(CadastroAdminDTO request){
        CadastroAdminEntity admin = new CadastroAdminEntity();
        admin.setNome(request.nome());
        admin.setEmail(request.email());
        admin.setSenha(passwordEncoder.encode(request.senha()));
        admin.setRole(Role.ADMIN);


        CadastroAdminEntity salvo = cadastroAdminRepository.save(admin);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO,
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()
        ));
        return new CadastroAdminResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    @Transactional

    public void removerAdmin(Integer id){
        CadastroAdminEntity admin = cadastroAdminRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Admin nao foi encontrado"));
        cadastroAdminRepository.delete(admin);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.REMOVIDO, admin.getId(), admin.getNome(), admin.getEmail(), admin.getRole()));
    }

    @Transactional

    public CadastroAdminResponse atualizarAdmin(Integer id, CadastroAdminDTO request){
        CadastroAdminEntity admin = cadastroAdminRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Admin nao encontrado"));
        admin.setNome(request.nome());
        admin.setEmail(request.email());
        admin.setSenha(passwordEncoder.encode(request.senha()));
        CadastroAdminEntity salvo = cadastroAdminRepository.save(admin);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO, salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

        return new CadastroAdminResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }
}
