package com.tiendasmass.inventario.support;

import com.tiendasmass.inventario.db.Database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Crea una base SQLite temporal por test (con el mismo esquema y datos semilla que la
 * app real) para que las pruebas nunca lean ni escriban tiendas_mass.db.
 */
public final class TestDb {

    private TestDb() {
    }

    public static Path crear() throws IOException {
        Path archivo = Files.createTempFile("tiendas-mass-test-", ".db");
        Files.deleteIfExists(archivo); // SQLite crea el archivo al abrir la conexión
        Database.setDbUrl("jdbc:sqlite:" + archivo.toAbsolutePath());
        Database.initSchema();
        return archivo;
    }

    public static void limpiar(Path archivo) throws IOException {
        Database.resetDbUrl();
        Files.deleteIfExists(archivo);
    }
}
