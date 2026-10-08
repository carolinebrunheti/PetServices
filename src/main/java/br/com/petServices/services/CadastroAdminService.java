package br.com.petServices.services;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.petServices.dtos.CadastroAdminResponse;
import br.com.petServices.repositories.CadastroAdminRepository;


@Service 
public class CadastroAdminService {

    private final CadastroAdminRepository cadastroAdminRepository;

    public CadastroAdminService(CadastroAdminRepository cadastroAdminRepository) {
        this.cadastroAdminRepository = cadastroAdminRepository;
    }

    public List<CadastroAdminResponse> buscarTodos(){
        return cadastroAdminRepository.findAll().stream()
        .map(e -> new CadastroAdminResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole())).toList();
    }

        public CadastroAdminResponse buscarPorId(Integer id){
          var admin =  cadastroAdminRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("Cliente não encontrado com o ID: " + id));
             return new CadastroAdminResponse(admin.getId(), admin.getNome(), admin.getEmail(), admin.getRole());
        }
}
