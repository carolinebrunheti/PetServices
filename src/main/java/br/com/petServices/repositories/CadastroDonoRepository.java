package br.com.petServices.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import br.com.petServices.entities.CadastroDonoEntity;

@Repository 
public interface CadastroDonoRepository extends JpaRepository<CadastroDonoEntity, Integer> {
    
}
