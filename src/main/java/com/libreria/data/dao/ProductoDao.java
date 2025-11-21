package com.libreria.data.dao;

import com.libreria.core.models.Producto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Optional;

public interface ProductoDao {

    @SqlQuery("""
                SELECT * FROM productos
                WHERE (sku_interno = :query OR codigo_barras = :query)
                AND activo = 1
                LIMIT 1
            """)
    @RegisterConstructorMapper(Producto.class)
    Optional<Producto> buscarPorCodigoOSku(@Bind("query") String query);

    @SqlQuery("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre ASC")
    @RegisterConstructorMapper(Producto.class)
    java.util.List<Producto> listarActivos();
}