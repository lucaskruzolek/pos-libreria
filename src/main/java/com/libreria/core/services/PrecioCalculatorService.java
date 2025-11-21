package com.libreria.core.services;

import com.libreria.core.exceptions.PrecioNoConfiguradoException;
import com.libreria.core.models.dto.PrecioRequest;
import com.libreria.core.models.dto.PrecioResult;
import com.libreria.data.dao.PreciosDao;
import org.jdbi.v3.core.Jdbi;

/**
 * SERVICIO DE CÁLCULO DE PRECIOS - CENTRO DE COPIADO
 * --------------------------------------------------
 * Responsabilidad: Determinar el costo unitario base de una impresión/fotocopia
 * consultando la matriz de precios configurada en SQLite.
 * * Stack: Java 21, JDBI 3, Records.
 */
public class PrecioCalculatorService {

    private final PreciosDao preciosDao;

    public PrecioCalculatorService(Jdbi jdbi) {
        this.preciosDao = jdbi.onDemand(PreciosDao.class);
    }

    /**
     * Calcula el precio unitario buscando la combinación exacta en la matriz.
     * @param request DTO con las características del servicio.
     * @return PrecioResult con el valor en centavos.
     * @throws PrecioNoConfiguradoException Si la combinación no existe en la DB.
     */
    public PrecioResult calcularPrecioBase(PrecioRequest request) {
        // Validación básica de entrada (Fail fast)
        if (request == null) {
            throw new IllegalArgumentException("El request de precio no puede ser nulo");
        }

        // Ejecución de consulta
        return preciosDao.buscarPrecio(
                        request.tamano().name(),
                        request.tipoPapel().name(),
                        request.color().name(),
                        request.faz().name()
                )
                .map(PrecioResult::new) // Transformamos el Integer a nuestro DTO de respuesta
                .orElseThrow(() -> new PrecioNoConfiguradoException(request));
    }
}
