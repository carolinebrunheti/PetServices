package br.com.petServices.services.enums.evento;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.petServices.services.enums.Especie;

public record PetEvent(
        TipoEvento tipoEvento,
        Integer id,
        String nome,
        String raca,
        LocalDate dataNascimento,
        Especie especie,
        Integer donoId,
        LocalDateTime ocorridoEm
) {
    public static PetEvent of(TipoEvento tipo, Integer id, String nome, String raca,
                              LocalDate dataNascimento, Especie especie, Integer donoId) {
        return new PetEvent(tipo, id, nome, raca, dataNascimento, especie, donoId, LocalDateTime.now());
    }
}