package com.tiendasmass.inventario.security;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de seguridad — almacenamiento de contraseñas (RNF03, OWASP A02:2021).
 * Verifica directamente en la fila cruda de la base de datos (no vía la API de la app)
 * que la contraseña en texto plano nunca queda persistida.
 */
class PasswordStorageTest {

    private Path dbFile;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = TestDb.crear();
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDb.limpiar(dbFile);
    }

    @Test
    void elHashAlmacenadoNoContieneLaClaveEnTextoPlano() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT password_hash FROM usuarios WHERE usuario = 'admin'")) {

            assertTrue(rs.next());
            String hashAlmacenado = rs.getString("password_hash");

            assertFalse(hashAlmacenado.contains("admin123"),
                    "La contraseña en texto plano no debe aparecer en la fila almacenada");
        }
    }

    @Test
    void elHashAlmacenadoTieneElFormatoSaltHashEsperado() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT password_hash FROM usuarios WHERE usuario = 'empleado'")) {

            assertTrue(rs.next());
            String hashAlmacenado = rs.getString("password_hash");

            assertTrue(hashAlmacenado.contains(":"), "Se espera el formato salt:hash de PasswordUtil");
            String[] partes = hashAlmacenado.split(":");
            assertTrue(partes.length == 2 && !partes[0].isEmpty() && !partes[1].isEmpty());
        }
    }
}
