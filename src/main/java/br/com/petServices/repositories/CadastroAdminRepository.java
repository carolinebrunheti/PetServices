package br.com.petServices.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import br.com.petServices.entities.CadastroAdminEntity;

@Repository 
public interface CadastroAdminRepository extends JpaRepository<CadastroAdminEntity, Integer>{
}
