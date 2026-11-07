package br.com.petServices.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.petServices.services.enums.Especie;
import br.com.petServices.services.enums.evento.PetEvent;
import br.com.petServices.services.enums.evento.TipoEvento;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventoRepository repository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void gravaEventoComTipoEPayloadQueVoltamAoObjetoOriginal() {
        var service = new OutboxService(repository, jsonMapper);
        var evento = PetEvent.of(TipoEvento.CRIADO, 5, "Rex", "SRD",
                java.time.LocalDate.of(2020, 1, 2), Especie.values()[0], 9);

        service.registrar("pets-eventos", "PET-5", evento);

        ArgumentCaptor<OutboxEventoEntity> captor = ArgumentCaptor.forClass(OutboxEventoEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventoEntity salvo = captor.getValue();
        assertThat(salvo.getTopico()).isEqualTo("pets-eventos");
        assertThat(salvo.getChave()).isEqualTo("PET-5");
        assertThat(salvo.getTipo()).isEqualTo(PetEvent.class.getName());
        assertThat(salvo.getEnviadoEm()).isNull();
        assertThat(jsonMapper.readValue(salvo.getPayload(), PetEvent.class)).isEqualTo(evento);
    }
}
