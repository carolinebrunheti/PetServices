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
import br.com.petServices.dtos.CadastroAtendenteDTO;
import br.com.petServices.dtos.CadastroAtendenteResponse;
import br.com.petServices.services.CadastroAtendenteService;
import jakarta.validation.Valid;

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

    @PostMapping()
    public ResponseEntity<CadastroAtendenteResponse> salvarAtendente(@RequestBody @Valid CadastroAtendenteDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroAtendenteService.salvar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAtendente (@PathVariable @Valid Integer id){
        cadastroAtendenteService.removerAtendente(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CadastroAtendenteResponse> atualizarAtendente( @PathVariable @Valid Integer id, @RequestBody @Valid CadastroAtendenteDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroAtendenteService.atualizarAtendente(id, request));
    }
}
