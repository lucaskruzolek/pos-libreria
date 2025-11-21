package com.libreria.data.config;

import com.libreria.core.services.ConfiguracionService;
import com.libreria.core.services.PrecioCalculatorService;
import com.libreria.core.services.ProductoService;
import com.libreria.core.services.VentaService;
import org.jdbi.v3.core.Jdbi;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CONTENEDOR DE DEPENDENCIAS (Service Locator / Composition Root).
 * Responsabilidad: Centralizar la creación de objetos (Wiring).
 * Patrón: Singleton.
 */
public class AppContainer {

    private static AppContainer instance;

    public final ConfiguracionService configuracionService;
    public final VentaService ventaService;
    public final PrecioCalculatorService precioService;
    public final ProductoService productoService;
    private final ExecutorService executorService;

    private AppContainer() {
        Jdbi jdbi = DatabaseManager.get();

        this.configuracionService = new ConfiguracionService(jdbi);
        this.productoService = new ProductoService(jdbi);
        this.ventaService = new VentaService(jdbi);
        this.precioService = new PrecioCalculatorService(jdbi);
        this.executorService = Executors.newCachedThreadPool();
    }

    public static synchronized AppContainer getInstance() {
        if (instance == null) {
            instance = new AppContainer();
        }
        return instance;
    }

    public ConfiguracionService getConfiguracionService() {
        return configuracionService;
    }

    public VentaService getVentaService() {
        return ventaService;
    }

    public PrecioCalculatorService getPrecioService() {
        return precioService;
    }

    public ProductoService getProductoService() {
        return productoService;
    }

    public ExecutorService getExecutor() {
        return executorService;
    }
}
