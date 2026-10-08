package br.com.petServices.controllers;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.petServices.dtos.CadastroAdminResponse;
import br.com.petServices.services.CadastroAdminService;

@RestController 
@RequestMapping ("/admin")
public class CadastroAdminController {

    private final CadastroAdminService cadastroAdminService;

    public CadastroAdminController(CadastroAdminService cadastroAdminService) {
        this.cadastroAdminService = cadastroAdminService;
    }
    
    @GetMapping()
    public ResponseEntity<List<CadastroAdminResponse>> buscarTodosAdmins(){
        return ResponseEntity.ok(cadastroAdminService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroAdminResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroAdminService.buscarPorId(id));
    }
}
