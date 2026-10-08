package br.com.petServices.controllers;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.petServices.dtos.CadastroVeterinarioResponse;
import br.com.petServices.services.CadastroVeterinarioService;

@RestController
@RequestMapping("/veterinario")
public class CadastroVeterinarioController {

    private final CadastroVeterinarioService cadastroVeterinarioService;

    public CadastroVeterinarioController(CadastroVeterinarioService cadastroVeterinarioService) {
        this.cadastroVeterinarioService = cadastroVeterinarioService;
    }

    @GetMapping()
    public ResponseEntity<List<CadastroVeterinarioResponse>> buscarTodos(){
        return ResponseEntity.ok(cadastroVeterinarioService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroVeterinarioResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroVeterinarioService.buscarPorId(id));
    }
}
