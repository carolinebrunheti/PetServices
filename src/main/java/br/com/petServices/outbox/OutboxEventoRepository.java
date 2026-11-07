package br.com.petServices.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventoRepository extends JpaRepository<OutboxEventoEntity, Long> {

    /**
     * Lock do Postgres que vale ate o fim da transacao. So uma replica do petservices
     * publica por vez, e assim os eventos saem na ordem em que foram gravados.
     * Devolve false (sem esperar) se outra replica ja estiver publicando.
     */
    @Query(value = "SELECT pg_try_advisory_xact_lock(74280001)", nativeQuery = true)
    boolean tentarLockDoPublicador();

    @Query(value = "SELECT * FROM tb_outbox_evento WHERE enviado_em IS NULL ORDER BY id LIMIT :limite",
            nativeQuery = true)
    List<OutboxEventoEntity> buscarPendentes(@Param("limite") int limite);

    @Modifying
    @Query("DELETE FROM OutboxEventoEntity e WHERE e.enviadoEm < :limite")
    int apagarEnviadosAntesDe(@Param("limite") LocalDateTime limite);
}
