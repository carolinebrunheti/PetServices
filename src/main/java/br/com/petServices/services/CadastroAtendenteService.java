package br.com.petServices.services;
import java.util.List;
import org.springframework.stereotype.Service;
import br.com.petServices.dtos.CadastroAtendenteResponse;
import br.com.petServices.repositories.CadastroAtendenteRepository;

@Service 
public class CadastroAtendenteService {

    private final CadastroAtendenteRepository cadastroAtendenteRepository;

    public CadastroAtendenteService(CadastroAtendenteRepository cadastroAtendenteRepository) {
        this.cadastroAtendenteRepository = cadastroAtendenteRepository;
    }

    public List<CadastroAtendenteResponse> buscarTodos(){
        return cadastroAtendenteRepository.findAll().stream()
        .map(e -> new CadastroAtendenteResponse(e.getId(), e.getNome(), e.getEmail(), e.getRole())).toList();
    }

    public CadastroAtendenteResponse buscarPorId(Integer id){
        var atendente = cadastroAtendenteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Atendente não encontrado com o ID: " + id));
        return new CadastroAtendenteResponse(atendente.getId(), atendente.getNome(), atendente.getEmail(), atendente.getRole());
    }
}
