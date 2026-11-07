package br.com.petServices.services.enums.evento;

import org.springframework.stereotype.Component;

import br.com.petServices.config.KafkaTopicConfig;
import br.com.petServices.outbox.OutboxService;
import lombok.RequiredArgsConstructor;

/**
 * Nao envia direto para o Kafka: grava o evento na outbox, na mesma transacao do
 * cadastro. Quem publica de verdade e o OutboxPublisher.
 */
@Component
@RequiredArgsConstructor
public class UsuarioEventProducer {

    private final OutboxService outboxService;

    public void publicar(UsuarioEvent evento) {
        String chave = evento.role() + "-" + evento.id();
        outboxService.registrar(KafkaTopicConfig.USUARIOS_EVENTOS, chave, evento);
    }
}
