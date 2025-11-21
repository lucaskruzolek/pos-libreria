package com.libreria.core.models;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public record Producto(
        Integer id,
        @ColumnName("categoria_id") Integer categoriaId,
        @ColumnName("codigo_barras") String codigoBarras,
        @ColumnName("sku_interno") String skuInterno,
        String nombre,
        String tipo,
        @ColumnName("precio_base_centavos") Integer precioBaseCentavos,
        @ColumnName("stock_actual") Integer stockActual) {
    public boolean esFisico() {
        return "FISICO".equalsIgnoreCase(tipo);
    }

    public boolean esServicio() {
        return "SERVICIO".equalsIgnoreCase(tipo);
    }

    public Integer getCosto() {
        return precioBaseCentavos;
    }

    public Integer getPrecioEfectivo(double margen) {
        if (precioBaseCentavos == null)
            return 0;
        return (int) Math.round(precioBaseCentavos * margen);
    }

    public Integer getPrecioTransferencia(double margen) {
        if (precioBaseCentavos == null)
            return 0;
        return (int) Math.round(precioBaseCentavos * margen);
    }
}
