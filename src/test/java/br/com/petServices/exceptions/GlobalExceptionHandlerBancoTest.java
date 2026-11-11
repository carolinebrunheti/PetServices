package br.com.petServices.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;

class GlobalExceptionHandlerBancoTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/teste");

    @Test
    void poolSemConexaoViraServicoIndisponivel() {
        var ex = new DataAccessResourceFailureException("Unable to acquire JDBC Connection",
                new SQLTransientConnectionException("Connection is not available, request timed out after 3000ms"));

        var resposta = handler.erroInesperado(ex, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(503);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
        assertThat(resposta.getBody().status()).isEqualTo(503);
    }

    @Test
    void transacaoQueNaoAbreViraServicoIndisponivel() {
        var resposta = handler.erroInesperado(new CannotCreateTransactionException("Could not open JPA EntityManager"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void conexaoDerrubadaPeloServidorViraServicoIndisponivel() {
        // o que aconteceu no teste: "terminating connection due to administrator command" (SQLState 57P01)
        var ex = new JpaSystemException(new RuntimeException("rollback",
                new SQLException("FATAL: terminating connection due to administrator command", "57P01")));

        assertThat(handler.erroInesperado(ex, request).getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void erroDeBancoQueNaoEConexaoContinuaSendo500() {
        // ex.: violacao de constraint e bug/dado, nao indisponibilidade: nao deve virar 503
        var ex = new DataIntegrityViolationException("duplicate key",
                new SQLException("duplicate key value violates unique constraint", "23505"));

        assertThat(handler.erroInesperado(ex, request).getStatusCode().value()).isEqualTo(500);
    }
}
