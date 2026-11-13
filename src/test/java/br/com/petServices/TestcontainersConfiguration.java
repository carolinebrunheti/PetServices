package br.com.petServices;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Postgres e Kafka de verdade, em conteineres Docker, para os testes de integracao.
 * Mesmas versoes do cluster (PetServicesK8s/base). O @ServiceConnection faz o Spring
 * apontar spring.datasource.* e spring.kafka.bootstrap-servers para os conteineres,
 * entao o application.properties (localhost:5432/9092) nao e usado nos testes.
 * Os conteineres sobem uma vez e sao reaproveitados por todas as classes de teste.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka:4.3.0");
    }
}
