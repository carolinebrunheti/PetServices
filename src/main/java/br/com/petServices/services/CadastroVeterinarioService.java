package br.com.petServices.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.petServices.dtos.CadastroVeterinarioDTO;
import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.entities.CadastroVeterinarioEntity;
import br.com.petServices.exceptions.ConflitoException;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroVeterinarioRepository;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import br.com.petServices.services.enums.evento.UsuarioEventProducer;

@Service 
public class CadastroVeterinarioService {
    
    private final CadastroVeterinarioRepository cadastroVeterinarioRepository;
    private final UsuarioEventProducer usuarioEventProducer;
    private final PasswordEncoder passwordEncoder;


    public CadastroVeterinarioService(CadastroVeterinarioRepository cadastroVeterinarioRepository,
            UsuarioEventProducer usuarioEventProducer, PasswordEncoder passwordEncoder) {
        this.cadastroVeterinarioRepository = cadastroVeterinarioRepository;
        this.usuarioEventProducer = usuarioEventProducer;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<CadastroVeterinarioResponse> buscarTodos(Pageable pageable){
        return cadastroVeterinarioRepository.findAll(pageable)
        .map(e -> new CadastroVeterinarioResponse(e.getId(), e.getNome(), e.getCrm(), e.getEspecialidade(), e.getEmail(), e.getRole()));
    }

    public CadastroVeterinarioResponse buscarPorId(Integer id){
        var veterinario = cadastroVeterinarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veterinário não encontrado com o ID: " + id));
        return new CadastroVeterinarioResponse(veterinario.getId(), veterinario.getNome(), veterinario.getCrm(), veterinario.getEspecialidade(), veterinario.getEmail(), veterinario.getRole());
    }

    @Transactional

    public CadastroVeterinarioResponse salvar(CadastroVeterinarioDTO request){
        if (cadastroVeterinarioRepository.existsByEmail(request.email())) {
            throw new ConflitoException("Email ja cadastrado");
        }

        CadastroVeterinarioEntity veterinario = new CadastroVeterinarioEntity();
        veterinario.setNome(request.nome());
        veterinario.setEmail(request.email());
        veterinario.setEspecialidade(request.especialidade());
        veterinario.setCrm(request.crm());
        veterinario.setSenha(passwordEncoder.encode(request.senha()));
        veterinario.setRole(Role.VETERINARIO);

        CadastroVeterinarioEntity salvo = cadastroVeterinarioRepository.save(veterinario);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO,
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

        return new CadastroVeterinarioResponse(salvo.getId(), salvo.getNome(), salvo.getCrm(), salvo.getEspecialidade(), salvo.getEmail(), salvo.getRole());
    }

    @Transactional

    public void removerVeterinario(Integer id){
        CadastroVeterinarioEntity veterinario = cadastroVeterinarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veterinario nao existe"));
        cadastroVeterinarioRepository.delete(veterinario);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.REMOVIDO,
            veterinario.getId(), veterinario.getNome(), veterinario.getEmail(), veterinario.getRole()
        ));
    }

//atualizar
    @Transactional
    public CadastroVeterinarioResponse atualizarVeterinario(Integer id, CadastroVeterinarioDTO request){
        CadastroVeterinarioEntity veterinario = cadastroVeterinarioRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Vterinario nao encontrado"));

        veterinario.setNome(request.nome());
        veterinario.setEmail(request.email());
        veterinario.setCrm(request.crm());
        veterinario.setEspecialidade(request.especialidade());
        veterinario.setSenha(passwordEncoder.encode(request.senha()));

        CadastroVeterinarioEntity salvo = cadastroVeterinarioRepository.save(veterinario);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO, salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

        return new CadastroVeterinarioResponse(salvo.getId(), salvo.getNome(), salvo.getCrm(), salvo.getEspecialidade(), salvo.getEmail(), salvo.getRole());
    }
}
