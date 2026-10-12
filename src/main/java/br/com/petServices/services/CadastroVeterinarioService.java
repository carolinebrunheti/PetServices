package br.com.petServices.services;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.petServices.dtos.CadastroVeterinarioDTO;
import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.entities.CadastroVeterinarioEntity;
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


    public CadastroVeterinarioService(CadastroVeterinarioRepository cadastroVeterinarioRepository,
            UsuarioEventProducer usuarioEventProducer) {
        this.cadastroVeterinarioRepository = cadastroVeterinarioRepository;
        this.usuarioEventProducer = usuarioEventProducer;
    }

    public List<CadastroVeterinarioResponse> buscarTodos(){
        return cadastroVeterinarioRepository.findAll().stream()
        .map(e -> new CadastroVeterinarioResponse(e.getId(), e.getNome(), e.getCrm(), e.getEspecialidade(), e.getEmail(), e.getRole())).toList();
    }

    public CadastroVeterinarioResponse buscarPorId(Integer id){
        var veterinario = cadastroVeterinarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veterinário não encontrado com o ID: " + id));
        return new CadastroVeterinarioResponse(veterinario.getId(), veterinario.getNome(), veterinario.getCrm(), veterinario.getEspecialidade(), veterinario.getEmail(), veterinario.getRole());
    }

    public CadastroVeterinarioResponse salvar(CadastroVeterinarioDTO request){
        CadastroVeterinarioEntity veterinario = new CadastroVeterinarioEntity();
        veterinario.setNome(request.nome());
        veterinario.setEmail(request.email());
        veterinario.setEspecialidade(request.especialidade());
        veterinario.setCrm(request.crm());
        veterinario.setSenha(request.senha());
        veterinario.setRole(Role.VETERINARIO);

        CadastroVeterinarioEntity salvo = cadastroVeterinarioRepository.save(veterinario);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.CRIADO,
            salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

        return new CadastroVeterinarioResponse(salvo.getId(), salvo.getNome(), salvo.getCrm(), salvo.getEspecialidade(), salvo.getEmail(), salvo.getRole());
    }

    public void removerVeterinario(Integer id){
        CadastroVeterinarioEntity veterinario = cadastroVeterinarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veterinario nao existe"));
        cadastroVeterinarioRepository.delete(veterinario);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.REMOVIDO,
            veterinario.getId(), veterinario.getNome(), veterinario.getEmail(), veterinario.getRole()
        ));
    }

//atualizar
    public CadastroVeterinarioResponse atualizarVeterinario(Integer id, CadastroVeterinarioDTO request){
        CadastroVeterinarioEntity veterinario = cadastroVeterinarioRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Vterinario nao encontrado"));

        veterinario.setNome(request.nome());
        veterinario.setEmail(request.email());
        veterinario.setCrm(request.crm());
        veterinario.setEspecialidade(request.especialidade());
        veterinario.setSenha(request.senha());

        CadastroVeterinarioEntity salvo = cadastroVeterinarioRepository.save(veterinario);
        usuarioEventProducer.publicar(UsuarioEvent.of(TipoEvento.ATUALIZADO, salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole()));

        return new CadastroVeterinarioResponse(salvo.getId(), salvo.getNome(), salvo.getCrm(), salvo.getEspecialidade(), salvo.getEmail(), salvo.getRole());
    }
}
