package com.libreria.core.models.dto;

/**
 * DTO para la comunicación entre la UI (Tabla de ventas) y el Servicio.
 */
public record ItemCarrito(
        Integer productoId,
        String nombreProducto,
        Integer cantidad,
        Integer precioUnitarioCentavos,
        boolean esProductoFisico
) {
    public Integer calcularSubtotal() {
        return cantidad * precioUnitarioCentavos;
    }
}
