package br.com.petServices.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import br.com.petServices.entities.CadastroPetEntity;

@Repository 
public interface CadastroPetRepository extends JpaRepository<CadastroPetEntity, Integer> {

    List<CadastroPetEntity> findByDonoId(Integer donoId);
}
