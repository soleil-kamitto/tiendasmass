package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.DetalleVenta;
import com.tiendasmass.inventario.model.Producto;
import com.tiendasmass.inventario.model.Venta;
import com.tiendasmass.inventario.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF04/RF05/RNF10: registrar una venta debe descontar stock, y si algo falla la
 * operación completa debe revertirse (todo o nada).
 */
class VentaServiceTest {

    // Producto sembrado por Database.initSchema(): id=1, "Arroz Costeño 1kg", stock=40.
    private static final int PRODUCTO_ID = 1;
    private static final int STOCK_INICIAL = 40;

    private Path dbFile;
    private final VentaService ventaService = new VentaService();
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
    void registrarVentaDescuentaElStockVendido() {
        Venta venta = new Venta();
        venta.setClienteId(1);
        venta.setUsuarioId(1);
        venta.getDetalles().add(new DetalleVenta(PRODUCTO_ID, "Arroz Costeño 1kg", 5, new BigDecimal("4.50")));

        int ventaId = ventaService.registrarVenta(venta);

        assertEquals(STOCK_INICIAL - 5, buscarProducto(PRODUCTO_ID).getStock());
        assertEquals(new BigDecimal("22.50"), venta.getTotal());
        assertTrue(ventaId > 0);
    }

    @Test
    void registrarVentaConStockInsuficienteNoDescuentaNadaYPropagaError() {
        Venta venta = new Venta();
        venta.setClienteId(1);
        venta.setUsuarioId(1);
        venta.getDetalles().add(new DetalleVenta(PRODUCTO_ID, "Arroz Costeño 1kg", STOCK_INICIAL + 1, new BigDecimal("4.50")));

        assertThrows(RuntimeException.class, () -> ventaService.registrarVenta(venta));

        assertEquals(STOCK_INICIAL, buscarProducto(PRODUCTO_ID).getStock());
    }
}
