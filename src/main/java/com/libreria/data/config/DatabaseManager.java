package com.libreria.data.config;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DatabaseManager {

    // Cadena de conexión para SQLite local
    private static final String CONNECTION_STRING = "jdbc:sqlite:pos-db.sqlite";
    private static Jdbi jdbi;

    /**
     * Obtiene la instancia única de Jdbi configurada.
     */
    public static Jdbi get() {
        if (jdbi == null) {
            throw new IllegalStateException("DatabaseManager no ha sido inicializado. Llama a initDb() primero.");
        }
        return jdbi;
    }

    /**
     * Inicializa la conexión y asegura que las tablas existan.
     */
    public static void initDb() {
        System.out.println("[DB] Conectando a SQLite...");

        // 1. Crear instancia Jdbi
        jdbi = Jdbi.create(CONNECTION_STRING);

        // 2. Instalar Plugins (CRÍTICO para usar interfaces DAO @SqlQuery)
        jdbi.installPlugin(new SqlObjectPlugin());

        // 3. Ejecutar Schema
        System.out.println("[DB] Verificando esquema de tablas...");
        try (var handle = jdbi.open()) {
            String sql = loadSchemaSql();
            // Ejecutamos como script para permitir múltiples sentencias (create table,
            // index, etc)
            handle.createScript(sql).execute();
            System.out.println("[DB] Esquema verificado correctamente.");
        } catch (Exception e) {
            System.err.println("[DB] Error crítico inicializando la base de datos: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error inicializando la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Lee el archivo schema.sql desde los recursos del JAR/Classpath.
     */
    private static String loadSchemaSql() {
        String path = "/db/schema.sql";
        InputStream is = DatabaseManager.class.getResourceAsStream(path);

        if (is == null) {
            throw new RuntimeException("No se encontró el archivo de esquema en: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo schema.sql", e);
        }
    }
}
