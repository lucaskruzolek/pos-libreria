package com.libreria.core.services;

import com.libreria.core.models.DetalleVenta;
import com.libreria.core.models.Venta;
import com.libreria.core.models.dto.ItemCarrito;
import com.libreria.core.models.enums.EstadoFiscal;
import com.libreria.core.models.enums.MetodoPago;
import com.libreria.data.dao.VentaDao;
import com.libreria.core.models.Cliente;

import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.stream.Collectors;

public class VentaService {

        private final Jdbi jdbi;

        public VentaService(Jdbi jdbi) {
                this.jdbi = jdbi;
        }

        /**
         * Realiza una venta completa integrando lógica de stock y preparación fiscal.
         * * @param items Items del carrito.
         * 
         * @param pago            Metodo de pago seleccionado.
         * @param cliente         Cliente seleccionado (puede ser null).
         * @param requiereFactura true si el usuario solicitó Factura A/B.
         * @return El objeto Venta persistido (con ID).
         */
        public Venta realizarVenta(List<ItemCarrito> items, MetodoPago pago, Cliente cliente, boolean requiereFactura) {
                if (items == null || items.isEmpty())
                        throw new IllegalArgumentException("Carrito vacío");

                // 1. Calcular Total
                int total = items.stream().mapToInt(ItemCarrito::calcularSubtotal).sum();

                // 2. Determinar Estado Fiscal Inicial
                EstadoFiscal estadoFiscalInicial = requiereFactura ? EstadoFiscal.PENDIENTE : EstadoFiscal.NO_REQUIERE;

                // 3. Extraer datos del cliente (Snapshot)
                Integer clienteId = (cliente != null) ? cliente.id() : null;
                String cuitSnapshot = (cliente != null) ? cliente.cuit() : null;

                // 4. Construir Objeto Venta (Cabecera)
                Venta ventaNueva = Venta.crearNueva(
                                total, pago, clienteId, cuitSnapshot, requiereFactura, estadoFiscalInicial);

                // 5. Convertir Items de Carrito a Entidades de Detalle (Sin ID de venta aun)
                List<DetalleVenta> detalles = items.stream()
                                .map(item -> new DetalleVenta(
                                                null, null, // IDs null por ahora
                                                item.productoId(),
                                                item.cantidad(),
                                                item.precioUnitarioCentavos(),
                                                item.calcularSubtotal(),
                                                item.nombreProducto()))
                                .collect(Collectors.toList());

                // 6. PERSISTENCIA TRANSACCIONAL (ATOMICIDAD)
                long idGenerado = jdbi.inTransaction(handle -> {
                        VentaDao ventaDao = handle.attach(VentaDao.class);

                        // A. Insertar Venta y Detalles
                        long id = ventaDao.crearVentaCompleta(ventaNueva, detalles);

                        // B. Descontar Stock (Batch Update)
                        // Filtramos solo los productos físicos para el batch
                        List<DetalleVenta> itemsFisicos = items.stream()
                                        .filter(ItemCarrito::esProductoFisico)
                                        .map(item -> new DetalleVenta(
                                                        null, null,
                                                        item.productoId(),
                                                        item.cantidad(),
                                                        0, 0, null // Solo nos importa ID y Cantidad para el stock
                        ))
                                        .collect(Collectors.toList());

                        if (!itemsFisicos.isEmpty()) {
                                ventaDao.descontarStockBatch(itemsFisicos);
                        }

                        return id;
                });

                System.out.println("Venta registrada ID: " + idGenerado + " [Fiscal: " + estadoFiscalInicial + "]");

                // Retornamos una copia con el ID asignado
                return new Venta(
                                (int) idGenerado, ventaNueva.fechaCreacion(), total, pago, ventaNueva.estado(),
                                clienteId, cuitSnapshot, requiereFactura, estadoFiscalInicial,
                                null, null, 1, null, detalles);
        }
}
