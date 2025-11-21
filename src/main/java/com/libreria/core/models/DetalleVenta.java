package com.libreria.core.models;

public record DetalleVenta(
        Integer id,
        Integer ventaId,
        Integer productoId,
        Integer cantidad,
        Integer precioUnitarioCentavos,
        Integer subtotalCentavos,
        String descripcionLinea
) {
    // Helper para asignar ID de venta a un detalle existente
    public DetalleVenta withVentaId(Integer nuevoVentaId) {
        return new DetalleVenta(id, nuevoVentaId, productoId, cantidad, precioUnitarioCentavos, subtotalCentavos, descripcionLinea);
    }
}
