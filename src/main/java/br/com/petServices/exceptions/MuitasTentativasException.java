package br.com.petServices.exceptions;

public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException(String mensagem){
        super(mensagem);
    }

}
