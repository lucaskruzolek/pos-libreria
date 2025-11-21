package com.libreria.data.dao;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Optional;

/**
 * Interfaz DAO para JDBI.
 * Mapea las llamadas Java directamente a SQL.
 */
public interface PreciosDao {

    @SqlQuery("""
            SELECT precio_centavos 
            FROM matriz_precios 
            WHERE tamano = :tamano 
              AND tipo_papel = :tipoPapel 
              AND color = :color 
              AND faz = :faz
            LIMIT 1
            """)
    Optional<Integer> buscarPrecio(
            @Bind("tamano") String tamano,
            @Bind("tipoPapel") String tipoPapel,
            @Bind("color") String color,
            @Bind("faz") String faz
    );
}
