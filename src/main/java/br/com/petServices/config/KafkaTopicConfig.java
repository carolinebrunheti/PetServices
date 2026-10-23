package br.com.petServices.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String USUARIOS_EVENTOS = "usuarios-eventos";
    public static final String PETS_EVENTOS = "pets-eventos";

    @Value("${app.kafka.topic.replicas:1}")
    private int replicas;

    @Value("${app.kafka.topic.min-insync-replicas:1}")
    private String minInsyncReplicas;

    @Bean
    public NewTopic usuariosEventosTopic() {
        return TopicBuilder.name(USUARIOS_EVENTOS)
                .partitions(3)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }

    @Bean
    public NewTopic petsEventosTopic() {
        return TopicBuilder.name(PETS_EVENTOS)
                .partitions(3)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }
}
