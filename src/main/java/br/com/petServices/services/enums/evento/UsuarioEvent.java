package br.com.petServices.services.enums.evento;

import java.time.LocalDateTime;

import br.com.petServices.services.enums.Role;

public record UsuarioEvent(
        TipoEvento tipoEvento,
        Integer id,
        String nome,
        String email,
        Role role,
        LocalDateTime ocorridoEm
) {
    public static UsuarioEvent of(TipoEvento tipo, Integer id, String nome, String email, Role role) {
        return new UsuarioEvent(tipo, id, nome, email, role, LocalDateTime.now());
    }
}