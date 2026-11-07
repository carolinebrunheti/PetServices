package br.com.petServices.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga os @Scheduled (usado pelo OutboxPublisher). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
