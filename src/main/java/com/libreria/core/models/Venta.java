package com.libreria.core.models;

import com.libreria.core.models.enums.EstadoFiscal;
import com.libreria.core.models.enums.EstadoVenta;
import com.libreria.core.models.enums.MetodoPago;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Venta(
        Integer id,
        LocalDateTime fechaCreacion,
        Integer totalCentavos,
        MetodoPago metodoPago,
        EstadoVenta estado,

        // Datos Cliente y Fiscales
        Integer clienteId,
        String cuitCliente,
        boolean requiereFactura,
        EstadoFiscal estadoFiscal,

        // Datos de respuesta AFIP (se llenan post-facturación)
        String cae,
        LocalDate vtoCae,
        Integer puntoVenta,
        Integer numeroFactura,

        // Relación
        List<DetalleVenta> detalles) {
    // Helper para crear una venta nueva sin ID ni datos de AFIP
    public static Venta crearNueva(Integer total, MetodoPago pago, Integer clienteId, String cuit,
            boolean requiereFactura, EstadoFiscal estadoFiscal) {
        return new Venta(null, LocalDateTime.now(), total, pago, EstadoVenta.COMPLETADA,
                clienteId, cuit, requiereFactura, estadoFiscal,
                null, null, 1, null, List.of());
    }
}
