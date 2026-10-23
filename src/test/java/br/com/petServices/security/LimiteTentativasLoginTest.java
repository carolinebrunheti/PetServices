package br.com.petServices.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.petServices.exceptions.MuitasTentativasException;

class LimiteTentativasLoginTest {

    private static final String IP = "10.0.0.1";

    // relogio que o teste consegue adiantar
    private static class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-10-23T12:00:00Z");

        void adiantar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimiteTentativasLogin limite = new LimiteTentativasLogin(3, Duration.ofMinutes(15), relogio);

    @Test
    @DisplayName("abaixo do limite de falhas o IP continua podendo tentar")
    void abaixoDoLimiteNaoBloqueia() {
        // arrange
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);

        // act + assert
        assertDoesNotThrow(() -> limite.verificar(IP));
    }

    @Test
    @DisplayName("ao atingir o limite de falhas o IP fica bloqueado")
    void noLimiteBloqueia() {
        // arrange
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);

        // act + assert
        assertThrows(MuitasTentativasException.class, () -> limite.verificar(IP));
        assertDoesNotThrow(() -> limite.verificar("10.0.0.2"));
    }

    @Test
    @DisplayName("o bloqueio acaba depois do tempo configurado")
    void bloqueioVence() {
        // arrange
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);

        // act
        relogio.adiantar(Duration.ofMinutes(15));

        // assert
        assertDoesNotThrow(() -> limite.verificar(IP));
    }

    @Test
    @DisplayName("falhas antigas, fora da janela, nao contam para o bloqueio")
    void falhasForaDaJanelaNaoContam() {
        // arrange
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);
        relogio.adiantar(Duration.ofMinutes(16));

        // act
        limite.registrarFalha(IP);

        // assert
        assertDoesNotThrow(() -> limite.verificar(IP));
    }

    @Test
    @DisplayName("login com sucesso zera as falhas do IP")
    void sucessoZeraFalhas() {
        // arrange
        limite.registrarFalha(IP);
        limite.registrarFalha(IP);

        // act
        limite.registrarSucesso(IP);
        limite.registrarFalha(IP);

        // assert
        assertDoesNotThrow(() -> limite.verificar(IP));
    }
}
