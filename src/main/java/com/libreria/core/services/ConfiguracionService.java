package com.libreria.core.services;

import com.libreria.data.dao.ConfiguracionDao;
import org.jdbi.v3.core.Jdbi;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionService {
    private final ConfiguracionDao dao;
    private final Map<String, String> cache = new HashMap<>();

    public ConfiguracionService(Jdbi jdbi) {
        this.dao = jdbi.onDemand(ConfiguracionDao.class);
        // Cargar inicialmente
        try {
            recargarCache();
        } catch (Exception e) {
            // Si falla al inicio (ej: tabla no existe aun en tests), usar defaults
            System.err.println("Advertencia: No se pudo cargar la configuración inicial: " + e.getMessage());
        }
    }

    public void recargarCache() {
        cache.clear();
        dao.listarTodas().forEach(c -> cache.put(c.clave(), c.valor()));
    }

    public double getMargenEfectivo() {
        return Double.parseDouble(cache.getOrDefault("MARGEN_EFECTIVO", "1.50"));
    }

    public double getMargenTransferencia() {
        return Double.parseDouble(cache.getOrDefault("MARGEN_TRANSFERENCIA", "1.52"));
    }

    public void actualizarConfiguracion(String clave, String valor) {
        dao.actualizar(valor, clave);
        recargarCache();
    }
}
