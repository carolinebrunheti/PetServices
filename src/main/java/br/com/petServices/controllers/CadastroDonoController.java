package br.com.petServices.controllers;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.petServices.dtos.CadastroDonoDTO;
import br.com.petServices.dtos.CadastroDonoResponse;
import br.com.petServices.services.CadastroDonoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/dono")
public class CadastroDonoController {

    private final CadastroDonoService cadastroDonoService;

    public CadastroDonoController(CadastroDonoService cadastroDonoService) {
        this.cadastroDonoService = cadastroDonoService;
    }
    
    @GetMapping()
    public ResponseEntity<List<CadastroDonoResponse>> buscarTodos(){
        return ResponseEntity.ok(cadastroDonoService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroDonoResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroDonoService.buscarPorId(id));
    }

    @PostMapping()
    public ResponseEntity<CadastroDonoResponse> cadastrarDono(@RequestBody @Valid CadastroDonoDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroDonoService.cadastrar(request));
    }

    @PutMapping ("/{id}")
    public ResponseEntity<CadastroDonoResponse> atualizarDono (@RequestBody @Valid CadastroDonoDTO request, @PathVariable Integer id){
        return ResponseEntity.ok(cadastroDonoService.atualizar(request, id));
    }
}
