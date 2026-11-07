package br.com.petServices.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Grava o evento na tabela de outbox. Precisa rodar dentro da transacao do cadastro
 * (MANDATORY): se for chamado sem transacao, falha, em vez de gravar o evento solto.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventoRepository repository;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String topico, String chave, Object evento) {
        String payload = jsonMapper.writeValueAsString(evento);
        repository.save(new OutboxEventoEntity(topico, chave, evento.getClass().getName(), payload));
    }
}
