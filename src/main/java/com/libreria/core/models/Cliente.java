package com.libreria.core.models;

import java.time.LocalDateTime;

public record Cliente(
        Integer id,
        String razonSocial,
        String cuit,
        CondicionIva condicionIva,
        String email,
        String direccion,
        LocalDateTime fechaAlta
) {
    // Enum simple para restringir valores de texto en DB
    public enum CondicionIva {
        CONSUMIDOR_FINAL,
        RESPONSABLE_INSCRIPTO,
        MONOTRIBUTO,
        EXENTO
    }
}
