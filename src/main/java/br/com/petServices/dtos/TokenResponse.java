package br.com.petServices.dtos;

import java.time.Instant;

public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm
) {}
