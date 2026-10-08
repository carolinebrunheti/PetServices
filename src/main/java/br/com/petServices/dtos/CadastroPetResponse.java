package br.com.petServices.dtos;
import java.time.LocalDate;
import br.com.petServices.services.enums.Especie;

public record CadastroPetResponse(
        Integer id,
        String nome,
        LocalDate dataNascimento,
        String raca,
        Especie especie,
        Integer donoId
){}
