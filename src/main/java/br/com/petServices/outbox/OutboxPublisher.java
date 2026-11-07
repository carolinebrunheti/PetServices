package br.com.petServices.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le os eventos pendentes da outbox e publica no Kafka, na ordem em que foram gravados.
 * Se um envio falhar (ex.: Kafka fora), para o lote ali: os seguintes ficam esperando a
 * proxima rodada, para nao publicar um ATUALIZADO antes do CRIADO do mesmo cadastro.
 * Garantia: at-least-once (se o pod morrer depois do envio e antes do commit, o evento
 * sai de novo), por isso os consumidores devem ser idempotentes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    static final String PACOTE_PERMITIDO = "br.com.petServices.";

    private final OutboxEventoRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${app.outbox.tamanho-lote:100}")
    private int tamanhoLote = 100;

    @Value("${app.outbox.timeout-envio-ms:10000}")
    private long timeoutEnvioMs = 10000;

    @Value("${app.outbox.dias-retencao:7}")
    private int diasRetencao = 7;

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    @Transactional
    public void publicarPendentes() {
        if (!repository.tentarLockDoPublicador()) {
            return; // outra replica esta publicando
        }
        List<OutboxEventoEntity> pendentes = repository.buscarPendentes(tamanhoLote);
        int enviados = 0;
        for (OutboxEventoEntity evento : pendentes) {
            try {
                kafkaTemplate.send(evento.getTopico(), evento.getChave(), desserializar(evento))
                        .get(timeoutEnvioMs, TimeUnit.MILLISECONDS);
                evento.setEnviadoEm(LocalDateTime.now());
                enviados++;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Throwable causa = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
                evento.setTentativas(evento.getTentativas() + 1);
                evento.setUltimoErro(resumir(causa.toString()));
                log.warn("Outbox: falha ao publicar evento {} ({}), tentativa {}. {} evento(s) ficam para a proxima rodada. Erro: {}",
                        evento.getId(), evento.getChave(), evento.getTentativas(),
                        pendentes.size() - enviados, causa.toString());
                break;
            }
        }
        if (enviados > 0) {
            log.info("Outbox: {} evento(s) publicado(s)", enviados);
        }
    }

    /** Apaga os eventos ja enviados ha mais de N dias, para a tabela nao crescer para sempre. */
    @Scheduled(cron = "${app.outbox.cron-limpeza:0 0 3 * * *}")
    @Transactional
    public void limparEnviados() {
        int apagados = repository.apagarEnviadosAntesDe(LocalDateTime.now().minusDays(diasRetencao));
        log.info("Outbox: {} evento(s) enviado(s) apagado(s)", apagados);
    }

    private Object desserializar(OutboxEventoEntity evento) throws ClassNotFoundException {
        if (!evento.getTipo().startsWith(PACOTE_PERMITIDO)) {
            throw new IllegalStateException("Tipo de evento nao permitido: " + evento.getTipo());
        }
        return jsonMapper.readValue(evento.getPayload(), Class.forName(evento.getTipo()));
    }

    private static String resumir(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }
}
