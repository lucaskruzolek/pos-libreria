package com.libreria.core.models.dto;

/**
 * Response Inmutable.
 * Devuelve centavos para manejo monetario seguro.
 */
public record PrecioResult(Integer precioCentavos) {
    public double getPrecioFormatoDecimal() {
        return precioCentavos / 100.0;
    }
}
