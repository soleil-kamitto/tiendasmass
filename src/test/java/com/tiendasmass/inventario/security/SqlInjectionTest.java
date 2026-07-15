package com.tiendasmass.inventario.security;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.dao.UsuarioDAO;
import com.tiendasmass.inventario.model.Producto;
import com.tiendasmass.inventario.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de seguridad — inyección SQL (OWASP A03:2021).
 * Todos los DAO usan PreparedStatement con parámetros, así que estos payloads
 * clásicos deben tratarse como texto literal y NUNCA alterar la consulta.
 */
class SqlInjectionTest {

    private Path dbFile;
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @BeforeEach
    void setUp() throws IOException {
        dbFile = TestDb.crear();
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDb.limpiar(dbFile);
    }

    @Test
    void busquedaDeProductosNoEsVulnerableAInyeccionSql() {
        // Payload clásico: si la consulta concatenara el texto, esto devolvería TODOS los productos.
        List<Producto> resultado = productoDAO.buscar("' OR '1'='1");

        assertTrue(resultado.isEmpty(), "El payload de inyección no debe devolver resultados");
    }

    @Test
    void busquedaDeProductosConPayloadDeDropTableNoRompeLaConsultaNiBorraDatos() {
        List<Producto> resultado = productoDAO.buscar("x'; DROP TABLE productos; --");

        assertTrue(resultado.isEmpty());
        // Si la tabla hubiera sido borrada, esta segunda llamada lanzaría SQLException.
        assertTrue(productoDAO.listarTodos().size() > 0, "La tabla productos debe seguir intacta");
    }

    @Test
    void loginNoEsVulnerableAInyeccionSqlParaBypassearAutenticacion() {
        // Payload típico para bypass de login: admin' --
        Optional<?> resultado = usuarioDAO.buscarPorUsuario("admin' -- ");

        assertTrue(resultado.isEmpty(), "El payload no debe hacer match con el usuario admin real");
    }
}
