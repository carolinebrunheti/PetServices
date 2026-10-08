package br.com.petServices.services;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.repositories.CadastroVeterinarioRepository;

@Service 
public class CadastroVeterinarioService {
    
    private final CadastroVeterinarioRepository cadastroVeterinarioRepository;

    public CadastroVeterinarioService(CadastroVeterinarioRepository cadastroVeterinarioRepository) {
        this.cadastroVeterinarioRepository = cadastroVeterinarioRepository;
    }


    public List<CadastroVeterinarioResponse> buscarTodos(){
        return cadastroVeterinarioRepository.findAll().stream()
        .map(e -> new CadastroVeterinarioResponse(e.getId(), e.getNome(), e.getCrm(), e.getEspecialidade(), e.getEmail(), e.getRole())).toList();
    }

    public CadastroVeterinarioResponse buscarPorId(Integer id){
        var veterinario = cadastroVeterinarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Veterinário não encontrado com o ID: " + id));
        return new CadastroVeterinarioResponse(veterinario.getId(), veterinario.getNome(), veterinario.getCrm(), veterinario.getEspecialidade(), veterinario.getEmail(), veterinario.getRole());
    }
}
