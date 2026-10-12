package br.com.petServices.controllers;

import java.util.List;
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
import br.com.petServices.dtos.CadastroPetDTO;
import br.com.petServices.dtos.CadastroPetResponse;
import br.com.petServices.services.CadastroPetService;
import jakarta.validation.Valid;

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

    @PostMapping()
    public ResponseEntity<CadastroPetResponse> salvarPet(@RequestBody @Valid CadastroPetDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroPetService.salvarPet(request)
        );
    }

    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> removerPet(@PathVariable @Valid Integer id){
        cadastroPetService.deletarPet(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}") 
    public ResponseEntity<CadastroPetResponse> atualizarPet(@PathVariable @Valid Integer id, @RequestBody @Valid CadastroPetDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroPetService.atualizarPet(id, request));
    }
}
