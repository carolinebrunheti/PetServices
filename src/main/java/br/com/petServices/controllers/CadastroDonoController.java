package br.com.petServices.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    public ResponseEntity<PagedModel<CadastroDonoResponse>> buscarTodos(@PageableDefault(size = 10, sort = "id") Pageable pageable){
        return ResponseEntity.ok(new PagedModel<>(cadastroDonoService.buscarTodos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroDonoResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroDonoService.buscarPorId(id));
    }

    @PostMapping()
    public ResponseEntity<CadastroDonoResponse> cadastrarDono(@RequestBody @Valid CadastroDonoDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroDonoService.cadastrar(request));
    }

    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> removerDono(@PathVariable @Valid Integer id){
        cadastroDonoService.removerDono(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping ("/{id}")
    public ResponseEntity<CadastroDonoResponse> atualizarDono (@RequestBody @Valid CadastroDonoDTO request, @PathVariable Integer id){
        return ResponseEntity.ok(cadastroDonoService.atualizar(request, id));
    }
}
