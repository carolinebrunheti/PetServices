package br.com.petServices.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.petServices.entities.CadastroVeterinarioEntity;

@Repository 
public interface CadastroVeterinarioRepository extends JpaRepository<CadastroVeterinarioEntity, Integer> {
    
    boolean existsByEmail(String email);

    Optional<CadastroVeterinarioEntity> findByEmail(String email);
}
