package br.com.petServices.exceptions;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
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

    @ExceptionHandler (Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado (Exception ex, HttpServletRequest request){
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", request, List.of());
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
