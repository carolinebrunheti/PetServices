package br.com.petServices.services.enums.evento;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import br.com.petServices.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioEventProducer {

    private final KafkaTemplate<String, UsuarioEvent> kafkaTemplate;

    public void publicar(UsuarioEvent evento) {
        String chave = evento.role() + "-" + evento.id();

        kafkaTemplate.send(KafkaTopicConfig.USUARIOS_EVENTOS, chave, evento)
                .whenComplete((resultado, erro) -> {
                    if (erro != null) {
                        log.error("Falha ao publicar evento {}: {}", chave, erro.getMessage());
                    } else {
                        log.info("Evento {} {} publicado na partição {}",
                                evento.tipoEvento(), chave,
                                resultado.getRecordMetadata().partition());
                    }
                });
    }
}