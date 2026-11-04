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
import br.com.petServices.dtos.CadastroAdminDTO;
import br.com.petServices.dtos.CadastroAdminResponse;
import br.com.petServices.services.CadastroAdminService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/admin")
public class CadastroAdminController {

    private final CadastroAdminService cadastroAdminService;

    public CadastroAdminController(CadastroAdminService cadastroAdminService) {
        this.cadastroAdminService = cadastroAdminService;
    }
    
    @GetMapping()
    public ResponseEntity<PagedModel<CadastroAdminResponse>> buscarTodosAdmins(@PageableDefault(size = 10, sort = "id") Pageable pageable){
        return ResponseEntity.ok(new PagedModel<>(cadastroAdminService.buscarTodos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroAdminResponse> buscarPorId(@PathVariable Integer id){
        return ResponseEntity.ok(cadastroAdminService.buscarPorId(id));
    }

    @PostMapping()
    public ResponseEntity<CadastroAdminResponse> salvarAdmin(@RequestBody @Valid CadastroAdminDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroAdminService.salvar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAdmin (@PathVariable @Valid Integer id){
        cadastroAdminService.removerAdmin(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<CadastroAdminResponse> atualizarAdmin(@PathVariable @Valid Integer id, @RequestBody @Valid CadastroAdminDTO request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroAdminService.atualizarAdmin(id, request));
    }
}
