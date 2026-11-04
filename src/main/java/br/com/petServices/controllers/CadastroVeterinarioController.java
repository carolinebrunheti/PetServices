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

import br.com.petServices.dtos.CadastroVeterinarioDTO;
import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.services.CadastroVeterinarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/veterinario")
public class CadastroVeterinarioController {

    private final CadastroVeterinarioService cadastroVeterinarioService;

    public CadastroVeterinarioController(CadastroVeterinarioService cadastroVeterinarioService) {
        this.cadastroVeterinarioService = cadastroVeterinarioService;
    }

    @GetMapping()
    public ResponseEntity<PagedModel<CadastroVeterinarioResponse>> buscarTodos(@PageableDefault(size = 10, sort = "id") Pageable pageable){
        return ResponseEntity.ok(new PagedModel<>(cadastroVeterinarioService.buscarTodos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroVeterinarioResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroVeterinarioService.buscarPorId(id));
    }

    @PostMapping()
    public ResponseEntity<CadastroVeterinarioResponse> salvarVeterinario (@RequestBody @Valid CadastroVeterinarioDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroVeterinarioService.salvar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarVeterinario(@PathVariable @Valid Integer id){
        cadastroVeterinarioService.removerVeterinario(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CadastroVeterinarioResponse> atualizarVeterinario (@PathVariable @Valid Integer id, @RequestBody @Valid CadastroVeterinarioDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroVeterinarioService.atualizarVeterinario(id, request));
    }
    
}
