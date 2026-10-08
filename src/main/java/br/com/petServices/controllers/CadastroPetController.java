package br.com.petServices.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.petServices.dtos.CadastroPetResponse;
import br.com.petServices.services.CadastroPetService;

@RestController 
@RequestMapping("/pet")
public class CadastroPetController {

    private final CadastroPetService cadastroPetService;

    public CadastroPetController(CadastroPetService cadastroPetService) {
        this.cadastroPetService = cadastroPetService;
    }

    @GetMapping()
    public ResponseEntity<List<CadastroPetResponse>> buscarTodos(){
        return ResponseEntity.ok(cadastroPetService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroPetResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroPetService.buscarPorId(id));
    }
}
