package br.com.petServices.services;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.petServices.dtos.CadastroPetResponse;
import br.com.petServices.repositories.CadastroPetRepository;

@Service
public class CadastroPetService {
    
    private final CadastroPetRepository cadastroPetRepository;

    public CadastroPetService(CadastroPetRepository cadastroPetRepository) {
        this.cadastroPetRepository = cadastroPetRepository;
    }

    public List<CadastroPetResponse> buscarTodos(){
        return cadastroPetRepository.findAll().stream()
        .map(e -> new CadastroPetResponse(e.getId(), e.getNome(), e.getDataNascimento(), e.getRaca(), e.getEspecie(), e.getDono().getId())).toList();
    }

    public CadastroPetResponse buscarPorId(Integer id){
        var pet = cadastroPetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pet não encontrado com o ID: " + id));
        return new CadastroPetResponse(pet.getId(), pet.getNome(), pet.getDataNascimento(), pet.getRaca(), pet.getEspecie(), pet.getDono().getId());
    }
}
