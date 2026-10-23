package br.com.petServices.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.petServices.dtos.CadastroAdminDTO;
import br.com.petServices.repositories.CadastroAdminRepository;
import br.com.petServices.services.CadastroAdminService;
import lombok.extern.slf4j.Slf4j;

// fora do profile dev o banco nasce sem nenhum usuario, e so um admin consegue cadastrar outros.
// este runner cria o primeiro admin a partir das variaveis ADMIN_EMAIL e ADMIN_SENHA
@Slf4j
@Component
public class AdminInicialRunner implements ApplicationRunner {

    private static final int TAMANHO_MINIMO_SENHA = 12;

    private final CadastroAdminRepository cadastroAdminRepository;
    private final CadastroAdminService cadastroAdminService;
    private final String email;
    private final String senha;

    public AdminInicialRunner(CadastroAdminRepository cadastroAdminRepository,
            CadastroAdminService cadastroAdminService,
            @Value("${admin-inicial.email:}") String email,
            @Value("${admin-inicial.senha:}") String senha) {
        this.cadastroAdminRepository = cadastroAdminRepository;
        this.cadastroAdminService = cadastroAdminService;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cadastroAdminRepository.count() > 0) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Nenhum admin cadastrado. Defina ADMIN_EMAIL e ADMIN_SENHA para criar o primeiro admin");
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException("ADMIN_SENHA precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
        }

        cadastroAdminService.salvar(new CadastroAdminDTO("Administrador", email, senha));
        log.info("Primeiro admin criado com o email {}", email);
    }
}
