package br.com.petServices.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import br.com.petServices.security.UsuarioLogado;
import br.com.petServices.services.CadastroPetService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/pet")
public class CadastroPetController {

    private final CadastroPetService cadastroPetService;

    public CadastroPetController(CadastroPetService cadastroPetService) {
        this.cadastroPetService = cadastroPetService;
    }

    // o tutor so enxerga os proprios pets; os outros perfis veem todos
    @GetMapping()
    public ResponseEntity<PagedModel<CadastroPetResponse>> buscarTodos(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "id") Pageable pageable){
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        if (usuario.isTutor()) {
            return ResponseEntity.ok(new PagedModel<>(cadastroPetService.buscarPorDono(usuario.id(), pageable)));
        }
        return ResponseEntity.ok(new PagedModel<>(cadastroPetService.buscarTodos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CadastroPetResponse> buscarPorId(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt){
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        CadastroPetResponse pet = cadastroPetService.buscarPorId(id);
        if (usuario.isTutor() && !pet.donoId().equals(usuario.id())) {
            throw new AccessDeniedException("Pet de outro tutor");
        }
        return ResponseEntity.ok(pet);
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
