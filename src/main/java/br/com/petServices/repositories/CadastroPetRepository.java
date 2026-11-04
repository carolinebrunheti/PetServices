package br.com.petServices.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import br.com.petServices.entities.CadastroPetEntity;

@Repository 
public interface CadastroPetRepository extends JpaRepository<CadastroPetEntity, Integer> {

    Page<CadastroPetEntity> findByDonoId(Integer donoId, Pageable pageable);
}
