package br.com.petServices.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.petServices.exceptions.MuitasTentativasException;

// protege o login contra forca bruta: conta os logins errados de cada IP e,
// passando do limite, recusa novas tentativas desse IP ate o bloqueio vencer.
// os contadores ficam na memoria, entao valem por instancia da aplicacao
@Component
public class LimiteTentativasLogin {

    // acima disso, limpa os IPs cujo contador ja venceu para o mapa nao crescer sem fim
    private static final int LIMPAR_ACIMA_DE = 10_000;

    // falhas dentro da janela atual; a janela termina em expiraEm
    private record Tentativas(int falhas, Instant expiraEm) {}

    private final Map<String, Tentativas> porIp = new ConcurrentHashMap<>();
    private final int maxFalhas;
    private final Duration bloqueio;
    private final Clock clock;

    @Autowired
    public LimiteTentativasLogin(@Value("${login.max-falhas:5}") int maxFalhas,
            @Value("${login.bloqueio-minutos:15}") long bloqueioMinutos) {
        this(maxFalhas, Duration.ofMinutes(bloqueioMinutos), Clock.systemUTC());
    }

    LimiteTentativasLogin(int maxFalhas, Duration bloqueio, Clock clock) {
        this.maxFalhas = maxFalhas;
        this.bloqueio = bloqueio;
        this.clock = clock;
    }

    public void verificar(String ip) {
        Tentativas tentativas = porIp.get(ip);
        if (tentativas != null && !vencida(tentativas, Instant.now(clock)) && tentativas.falhas() >= maxFalhas) {
            throw new MuitasTentativasException("Muitas tentativas de login. Tente novamente em alguns minutos");
        }
    }

    public void registrarFalha(String ip) {
        Instant agora = Instant.now(clock);
        porIp.compute(ip, (chave, atual) -> {
            if (atual == null || vencida(atual, agora)) {
                return new Tentativas(1, agora.plus(bloqueio));
            }
            int falhas = atual.falhas() + 1;
            // ao atingir o limite, o bloqueio conta a partir desta ultima falha
            Instant expiraEm = falhas >= maxFalhas ? agora.plus(bloqueio) : atual.expiraEm();
            return new Tentativas(falhas, expiraEm);
        });

        if (porIp.size() > LIMPAR_ACIMA_DE) {
            porIp.values().removeIf(tentativas -> vencida(tentativas, agora));
        }
    }

    public void registrarSucesso(String ip) {
        porIp.remove(ip);
    }

    private boolean vencida(Tentativas tentativas, Instant agora) {
        return !agora.isBefore(tentativas.expiraEm());
    }
}
