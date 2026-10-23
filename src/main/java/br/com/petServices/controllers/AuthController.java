package br.com.petServices.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.petServices.dtos.LoginDTO;
import br.com.petServices.dtos.TokenResponse;
import br.com.petServices.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // o limite de tentativas e por IP. Atras de proxy/load balancer, configure
    // server.forward-headers-strategy para que getRemoteAddr devolva o IP real do cliente
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginDTO request, HttpServletRequest http){
        return ResponseEntity.ok(authService.login(request, http.getRemoteAddr()));
    }
}
