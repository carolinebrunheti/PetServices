package br.com.petServices.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.petServices.dtos.CadastroAtendenteResponse;
import br.com.petServices.services.CadastroAtendenteService;

@RestController
@RequestMapping("/atendente")
public class CadastroAtendenteController {

    private final CadastroAtendenteService cadastroAtendenteService;

    public CadastroAtendenteController(CadastroAtendenteService cadastroAtendenteService) {
        this.cadastroAtendenteService = cadastroAtendenteService;
    }
    
    @GetMapping()
    public ResponseEntity<List<CadastroAtendenteResponse>> buscarTodos(){
        return ResponseEntity.ok(cadastroAtendenteService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroAtendenteResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroAtendenteService.buscarPorId(id));
    }
}
