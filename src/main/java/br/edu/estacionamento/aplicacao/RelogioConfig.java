package br.edu.estacionamento.aplicacao;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fornece o relógio usado para registrar entrada e saída. Injetá-lo (em vez de
 * chamar OffsetDateTime.now() direto) permite simular a passagem do tempo nos
 * testes.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
