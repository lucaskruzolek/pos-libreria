package com.libreria.core.models.dto;

/**
 * package com.libreria.core.models.dto;
 * 
 * /**
 * DTO para la comunicación entre la UI (Tabla de ventas) y el Servicio.
 */
public record ItemCarrito(
        Integer productoId,
        String nombreProducto,
        Integer cantidad,
        Integer precioListaCentavos,
        Integer precioEfectivoCentavos,
        boolean esProductoFisico) {
    public Integer calcularSubtotal(boolean esEfectivo) {
        return cantidad * (esEfectivo ? precioEfectivoCentavos : precioListaCentavos);
    }

    public Integer getPrecioUnitario(boolean esEfectivo) {
        return esEfectivo ? precioEfectivoCentavos : precioListaCentavos;
    }
}
