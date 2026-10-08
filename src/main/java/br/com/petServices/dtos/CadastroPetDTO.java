package br.com.petServices.dtos;

import java.time.LocalDate;
import br.com.petServices.services.enums.Especie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

public record CadastroPetDTO(
        @NotBlank String nome,
        @Past LocalDate dataNascimento,
        String raca,
        @NotNull Especie especie,
        @NotNull Integer donoId
) {
    
}
