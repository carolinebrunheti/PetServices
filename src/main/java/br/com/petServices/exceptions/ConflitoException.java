package br.com.petServices.exceptions;

public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem){
        super(mensagem);
    }
    
}
