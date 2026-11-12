package br.com.petServices.exceptions;

import java.time.LocalDateTime;
import java.util.List;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler (RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex,
        HttpServletRequest request) {
            return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler (RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio( RegraNegocioException ex, HttpServletRequest request){
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest request){
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    // login com email ou senha errados
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(BadCredentialsException ex, HttpServletRequest request){
        return montar(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos", request, List.of());
    }

    // IP bloqueado depois de muitos logins errados seguidos
    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroResponse> muitasTentativas(MuitasTentativasException ex, HttpServletRequest request){
        return montar(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, List.of());
    }

    // requisicao sem token, com token invalido ou expirado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException ex, HttpServletRequest request){
        return montar(HttpStatus.UNAUTHORIZED, "Autenticação necessária: envie um token válido no header Authorization", request, List.of());
    }

    // usuario logado, mas sem permissao para a rota ou para o recurso
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest request){
        return montar(HttpStatus.FORBIDDEN, "Acesso negado: você não tem permissão para este recurso", request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos na requisição", request, detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, "JSON inválido ou com formato incorreto", request, List.of());
    }

    // ?sort= com um campo que nao existe na entidade
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido: " + ex.getPropertyName(), request, List.of());
    }

    @ExceptionHandler (Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado (Exception ex, HttpServletRequest request){
        if (bancoIndisponivel(ex)) {
            // sem stacktrace: com o banco fora, seriam centenas de linhas iguais por segundo
            log.warn("Banco indisponivel em {}: {}", request.getRequestURI(), causaRaiz(ex));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "5")
                    .body(montar(HttpStatus.SERVICE_UNAVAILABLE,
                            "Servico temporariamente indisponivel. Tente novamente em alguns segundos.",
                            request, List.of()).getBody());
        }
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", request, List.of());
    }

    /**
     * Banco fora do ar, ou sem conexao livre no pool depois do connection-timeout:
     * e uma indisponibilidade temporaria, entao responde 503 (com Retry-After) e nao 500.
     * Procura na cadeia de causas, porque o Spring/Hibernate embrulham o erro do driver.
     */
    static boolean bancoIndisponivel(Throwable ex) {
        for (Throwable t = ex; t != null; t = (t.getCause() == t ? null : t.getCause())) {
            if (t instanceof DataAccessResourceFailureException
                    || t instanceof CannotCreateTransactionException
                    || t instanceof SQLTransientConnectionException
                    || t instanceof SQLNonTransientConnectionException) {
                return true;
            }
            // SQLState 08xxx = erro de conexao; 57Pxx = servidor desligando/reiniciando
            if (t instanceof SQLException sql && sql.getSQLState() != null
                    && (sql.getSQLState().startsWith("08") || sql.getSQLState().startsWith("57P"))) {
                return true;
            }
        }
        return false;
    }

    private static String causaRaiz(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem,
        HttpServletRequest request, List<String> detalhes){

            ErroResponse erro = new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                detalhes
            );
            return ResponseEntity.status(status).body(erro);
        }
}
