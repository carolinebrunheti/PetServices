package br.com.petServices.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import br.com.petServices.config.KafkaTopicConfig;
import br.com.petServices.services.enums.Role;
import br.com.petServices.services.enums.evento.TipoEvento;
import br.com.petServices.services.enums.evento.UsuarioEvent;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventoRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(repository, kafkaTemplate, jsonMapper);
    }

    private OutboxEventoEntity evento(long id, int donoId) {
        UsuarioEvent ev = UsuarioEvent.of(TipoEvento.CRIADO, donoId, "Carole", "carole@x.com", Role.TUTOR);
        OutboxEventoEntity e = new OutboxEventoEntity(KafkaTopicConfig.USUARIOS_EVENTOS, "TUTOR-" + donoId,
                UsuarioEvent.class.getName(), jsonMapper.writeValueAsString(ev));
        e.setId(id);
        return e;
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, Object>> sucesso() {
        return CompletableFuture.completedFuture(org.mockito.Mockito.mock(SendResult.class));
    }

    @Test
    void publicaPendentesEMarcaComoEnviados() {
        var e1 = evento(1, 10);
        var e2 = evento(2, 11);
        when(repository.tentarLockDoPublicador()).thenReturn(true);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(e1, e2));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(sucesso());

        publisher.publicarPendentes();

        ArgumentCaptor<Object> valor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.USUARIOS_EVENTOS), eq("TUTOR-10"), valor.capture());
        // o Kafka recebe o objeto original (mesmo formato de antes), nao o JSON em texto
        assertThat(valor.getValue()).isInstanceOf(UsuarioEvent.class);
        assertThat(((UsuarioEvent) valor.getValue()).id()).isEqualTo(10);
        assertThat(e1.getEnviadoEm()).isNotNull();
        assertThat(e2.getEnviadoEm()).isNotNull();
    }

    @Test
    void falhaNoEnvioParaOLoteParaManterAOrdem() {
        var e1 = evento(1, 10);
        var e2 = evento(2, 11);
        var e3 = evento(3, 12);
        when(repository.tentarLockDoPublicador()).thenReturn(true);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(e1, e2, e3));
        when(kafkaTemplate.send(anyString(), eq("TUTOR-10"), any())).thenReturn(sucesso());
        when(kafkaTemplate.send(anyString(), eq("TUTOR-11"), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Kafka fora")));

        publisher.publicarPendentes();

        assertThat(e1.getEnviadoEm()).isNotNull();
        assertThat(e2.getEnviadoEm()).isNull();
        assertThat(e2.getTentativas()).isEqualTo(1);
        assertThat(e2.getUltimoErro()).contains("Kafka fora");
        assertThat(e3.getEnviadoEm()).isNull();
        verify(kafkaTemplate, never()).send(anyString(), eq("TUTOR-12"), any());
    }

    @Test
    void erroSincronoNoSendTambemEhTratado() {
        var e1 = evento(1, 10);
        when(repository.tentarLockDoPublicador()).thenReturn(true);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(e1));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new TimeoutException("Topic usuarios-eventos not present in metadata after 5000 ms."));

        publisher.publicarPendentes();

        assertThat(e1.getEnviadoEm()).isNull();
        assertThat(e1.getTentativas()).isEqualTo(1);
    }

    @Test
    void naoPublicaSeOutraReplicaEstiverComOLock() {
        when(repository.tentarLockDoPublicador()).thenReturn(false);

        publisher.publicarPendentes();

        verify(repository, never()).buscarPendentes(anyInt());
        verify(kafkaTemplate, times(0)).send(anyString(), anyString(), any());
    }

    @Test
    void recusaTipoForaDoPacoteDaAplicacao() {
        var e1 = evento(1, 10);
        e1.setTipo("java.lang.ProcessBuilder");
        when(repository.tentarLockDoPublicador()).thenReturn(true);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(e1));

        publisher.publicarPendentes();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        assertThat(e1.getUltimoErro()).contains("nao permitido");
    }
}
