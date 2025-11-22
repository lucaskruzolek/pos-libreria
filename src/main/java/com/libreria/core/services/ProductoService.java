package com.libreria.core.services;

import com.libreria.data.dao.ProductoDao;
import com.libreria.core.models.Producto;
import org.jdbi.v3.core.Jdbi;
import java.util.Optional;
import java.util.List;

public class ProductoService {

    private final ProductoDao productoDao;

    public ProductoService(Jdbi jdbi) {
        // Inyección del DAO dentro del Servicio
        this.productoDao = jdbi.onDemand(ProductoDao.class);
    }

    /**
     * Busca un producto por SKU o Código de Barras.
     * Centraliza el acceso a datos de productos.
     */
    public Optional<Producto> buscarPorSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        return productoDao.buscarPorCodigoOSku(sku.trim());
    }

    public List<Producto> listarProductosActivos() {
        return productoDao.listarActivos();
    }

    public Optional<Producto> buscarPorId(Integer id) {
        return productoDao.buscarPorId(id);
    }
}