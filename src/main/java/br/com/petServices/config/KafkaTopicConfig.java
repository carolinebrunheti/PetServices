package br.com.petServices.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration 
public class KafkaTopicConfig {
    
    public static final String USUARIOS_EVENTOS = "usuarios-eventos";
    public static final String PETS_EVENTOS = "pets-eventos";

    @Bean 
    public NewTopic usuariosEventosTopic(){
        return TopicBuilder.name(USUARIOS_EVENTOS)
        .partitions(3)
        .replicas(1)
        .build();
    }

    @Bean 
    public NewTopic petsEventosTopic(){
        return TopicBuilder.name(PETS_EVENTOS)
        .partitions(3)
        .replicas(1)
        .build();
    }
}
