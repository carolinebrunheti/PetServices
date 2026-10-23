package br.com.petServices.repositories;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.petServices.entities.CadastroAtendenteEntity;

@Repository 
public interface CadastroAtendenteRepository extends JpaRepository<CadastroAtendenteEntity, Integer>{

    Optional<CadastroAtendenteEntity> findByEmail(String email);
}
