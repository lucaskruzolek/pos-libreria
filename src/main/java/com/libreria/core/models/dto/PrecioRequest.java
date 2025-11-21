package com.libreria.core.models.dto;


import com.libreria.core.models.enums.Color;
import com.libreria.core.models.enums.Faz;
import com.libreria.core.models.enums.Tamano;
import com.libreria.core.models.enums.TipoPapel;

/**
 * Request Inmutable (Java Record).
 * Usamos Enums para garantizar integridad referencial en el código.
 */
public record PrecioRequest(
        Tamano tamano,
        TipoPapel tipoPapel,
        Color color,
        Faz faz
) {
    // Constructor canónico compacto para validaciones extra si fuesen necesarias
    public PrecioRequest {
        if (tamano == null || tipoPapel == null || color == null || faz == null) {
            throw new IllegalArgumentException("Todos los campos de la matriz son obligatorios");
        }
    }
}
