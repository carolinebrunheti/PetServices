package br.com.petServices.exceptions;

public class RegraNegocioException extends RuntimeException{
    
    public RegraNegocioException(String mensagem){
        super(mensagem);
    }
}
