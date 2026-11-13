package br.com.petServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;

import br.com.petServices.outbox.OutboxEventoRepository;
import br.com.petServices.repositories.CadastroAdminRepository;

/**
 * Teste de integracao: a aplicacao inteira sobe contra Postgres e Kafka reais (Testcontainers),
 * sem o profile dev, ou seja, com o banco vazio como em producao.
 * Prova: as migrations do Flyway rodam e o Hibernate valida as tabelas; o primeiro admin e
 * criado a partir das propriedades; o login gera um token que as rotas protegidas aceitam;
 * e o cadastro grava na outbox, que o OutboxPublisher entrega de verdade no Kafka.
 */
@SpringBootTest(properties = {
        "jwt.secret=chave-de-teste-com-no-minimo-32-caracteres",
        "admin-inicial.email=" + PetServicesApplicationTests.ADMIN_EMAIL,
        "admin-inicial.senha=" + PetServicesApplicationTests.ADMIN_SENHA
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PetServicesApplicationTests {

    static final String ADMIN_EMAIL = "admin@teste.dev";
    static final String ADMIN_SENHA = "senha-do-admin-de-teste";

    @Autowired private MockMvc mockMvc;
    @Autowired private KafkaContainer kafka;
    @Autowired private CadastroAdminRepository adminRepository;
    @Autowired private OutboxEventoRepository outboxRepository;

    @Test
    void contextLoadsECriaOPrimeiroAdmin() {
        // subir o contexto ja prova que o Flyway criou as tabelas e o Hibernate as validou
        assertThat(adminRepository.count()).isEqualTo(1);
    }

    @Test
    void loginComSenhaErradaDa401() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(ADMIN_EMAIL, "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroDeDonoEPetChegaNoKafkaPelaOutbox() throws Exception {
        String token = loginAdmin();

        int donoId = idDaResposta(mockMvc.perform(post("/dono").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria Teste", "email": "maria@teste.dev", "senha": "123456"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        int petId = idDaResposta(mockMvc.perform(post("/pet").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Rex", "dataNascimento": "2020-01-01", "raca": "SRD",
                                 "especie": "CACHORRO", "donoId": %d}
                                """.formatted(donoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        // o OutboxPublisher (a cada 1 s) le a tabela e publica: os eventos aparecem nos topicos
        assertThat(lerEvento("usuarios-eventos", "TUTOR-" + donoId).value())
                .contains("\"tipoEvento\":\"CRIADO\"")
                .contains("\"email\":\"maria@teste.dev\"")
                .doesNotContain("123456"); // a senha nunca vai para o evento
        assertThat(lerEvento("pets-eventos", "PET-" + petId).value())
                .contains("\"nome\":\"Rex\"")
                .contains("\"donoId\":" + donoId);

        // e a outbox fica sem pendencias
        await().atMost(Duration.ofSeconds(30))
                .until(() -> outboxRepository.findAll().stream().allMatch(e -> e.getEnviadoEm() != null));
    }

    @Test
    void emailDuplicadoDa409ENaoGeraEvento() throws Exception {
        String token = loginAdmin();
        String corpo = """
                {"nome": "Joao Teste", "email": "joao@teste.dev", "senha": "123456"}
                """;
        mockMvc.perform(post("/dono").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        long eventosAntes = outboxRepository.count();

        mockMvc.perform(post("/dono").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict());

        assertThat(outboxRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void semTokenNaoCadastra() throws Exception {
        mockMvc.perform(post("/dono").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Sem Token", "email": "semtoken@teste.dev", "senha": "123456"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String loginAdmin() throws Exception {
        String resposta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return resposta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private String login(String email, String senha) {
        return """
                {"email": "%s", "senha": "%s"}
                """.formatted(email, senha);
    }

    private int idDaResposta(String json) {
        return Integer.parseInt(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    // le o topico desde o inicio ate achar a mensagem com essa chave
    private ConsumerRecord<String, String> lerEvento(String topico, String chave) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "teste-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topico));
            long limite = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < limite) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                    if (chave.equals(r.key())) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError("Evento " + chave + " nao apareceu no topico " + topico);
    }
}
