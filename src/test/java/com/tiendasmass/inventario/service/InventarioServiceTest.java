package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.MovimientoInventario;
import com.tiendasmass.inventario.model.Producto;
import com.tiendasmass.inventario.model.TipoMovimiento;
import com.tiendasmass.inventario.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF13/RF14: reposición de stock y su historial de movimientos. */
class InventarioServiceTest {

    // Producto sembrado por Database.initSchema(): id=1, "Arroz Costeño 1kg", stock=40.
    private static final int PRODUCTO_ID = 1;
    private static final int STOCK_INICIAL = 40;
    private static final int USUARIO_ADMIN_ID = 1;

    private Path dbFile;
    private final InventarioService inventarioService = new InventarioService();
    private final ProductoDAO productoDAO = new ProductoDAO();

    @BeforeEach
    void setUp() throws IOException {
        dbFile = TestDb.crear();
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDb.limpiar(dbFile);
    }

    private Producto buscarProducto(int id) {
        return productoDAO.listarTodos().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void reponerStockAumentaElStockYRegistraElMovimiento() {
        inventarioService.reponerStock(PRODUCTO_ID, 10, "Reposición de prueba", USUARIO_ADMIN_ID);

        assertEquals(STOCK_INICIAL + 10, buscarProducto(PRODUCTO_ID).getStock());

        List<MovimientoInventario> historial = inventarioService.historial();
        assertTrue(historial.stream().anyMatch(m ->
                m.getProductoId() == PRODUCTO_ID
                        && m.getTipo() == TipoMovimiento.ENTRADA
                        && m.getCantidad() == 10));
    }

    @Test
    void reponerStockConCantidadCeroLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> inventarioService.reponerStock(PRODUCTO_ID, 0, "motivo", USUARIO_ADMIN_ID));
    }

    @Test
    void reponerStockConCantidadNegativaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> inventarioService.reponerStock(PRODUCTO_ID, -5, "motivo", USUARIO_ADMIN_ID));
    }
}
