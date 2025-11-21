package com.libreria.data.dao;

import com.libreria.core.models.DetalleVenta;
import com.libreria.core.models.Venta;
import com.libreria.core.models.enums.EstadoFiscal;
import com.libreria.core.models.enums.EstadoVenta;
import com.libreria.core.models.enums.MetodoPago;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public interface VentaDao extends SqlObject {

    @Transaction
    default long crearVentaCompleta(Venta venta, List<DetalleVenta> detallesSinId) {
        long ventaId = insertarCabecera(
                venta.totalCentavos(),
                venta.metodoPago(),
                venta.estado(),
                venta.clienteId(),
                venta.cuitCliente(),
                venta.requiereFactura(),
                venta.estadoFiscal(),
                venta.puntoVenta());

        List<DetalleVenta> detallesConId = detallesSinId.stream()
                .map(d -> d.withVentaId((int) ventaId))
                .collect(Collectors.toList());

        insertarDetalles(detallesConId);

        return ventaId;
    }

    @SqlUpdate("""
                INSERT INTO ventas (
                    total_centavos, metodo_pago, estado,
                    cliente_id, cuit_cliente, requiere_factura, estado_fiscal,
                    punto_venta
                ) VALUES (
                    :totalCentavos, :metodoPago, :estado,
                    :clienteId, :cuitCliente, :requiereFactura, :estadoFiscal,
                    :puntoVenta
                )
            """)
    @GetGeneratedKeys
    long insertarCabecera(
            @Bind("totalCentavos") Integer totalCentavos,
            @Bind("metodoPago") MetodoPago metodoPago,
            @Bind("estado") EstadoVenta estado,
            @Bind("clienteId") Integer clienteId,
            @Bind("cuitCliente") String cuitCliente,
            @Bind("requiereFactura") boolean requiereFactura,
            @Bind("estadoFiscal") EstadoFiscal estadoFiscal,
            @Bind("puntoVenta") Integer puntoVenta);

    default void insertarDetalles(List<DetalleVenta> detalles) {
        String sql = """
                    INSERT INTO detalle_ventas (
                        venta_id, producto_id, cantidad,
                        precio_unitario_centavos, subtotal_centavos, descripcion_linea
                    ) VALUES (
                        :ventaId, :productoId, :cantidad,
                        :precioUnitarioCentavos, :subtotalCentavos, :descripcionLinea
                    )
                """;

        PreparedBatch batch = getHandle().prepareBatch(sql);

        for (DetalleVenta d : detalles) {
            batch.bind("ventaId", d.ventaId())
                    .bind("productoId", d.productoId())
                    .bind("cantidad", d.cantidad())
                    .bind("precioUnitarioCentavos", d.precioUnitarioCentavos())
                    .bind("subtotalCentavos", d.subtotalCentavos())
                    .bind("descripcionLinea", d.descripcionLinea())
                    .add();
        }

        batch.execute();
    }

    @SqlUpdate("UPDATE productos SET stock_actual = stock_actual - :cantidad WHERE id = :id AND tipo = 'FISICO'")
    void descontarStock(@Bind("id") Integer productoId, @Bind("cantidad") Integer cantidad);

    default void descontarStockBatch(List<DetalleVenta> detalles) {
        String sql = "UPDATE productos SET stock_actual = stock_actual - :cantidad WHERE id = :productoId AND tipo = 'FISICO'";
        PreparedBatch batch = getHandle().prepareBatch(sql);

        for (DetalleVenta d : detalles) {
            batch.bind("productoId", d.productoId())
                    .bind("cantidad", d.cantidad())
                    .add();
        }
        batch.execute();
    }
}