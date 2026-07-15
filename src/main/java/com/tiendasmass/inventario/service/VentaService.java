package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.MovimientoInventarioDAO;
import com.tiendasmass.inventario.dao.VentaDAO;
import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.model.DetalleVenta;
import com.tiendasmass.inventario.model.TipoMovimiento;
import com.tiendasmass.inventario.model.Venta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * RF04/RF05: registra una venta y descuenta el stock automáticamente en una sola transacción,
 * para que RNF10 (integridad de datos) se cumpla incluso si algo falla a mitad de camino.
 */
public class VentaService {

    private final VentaDAO ventaDAO = new VentaDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

    public int registrarVenta(Venta venta) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                validarStockDisponible(conn, venta);

                BigDecimal total = BigDecimal.ZERO;
                for (DetalleVenta d : venta.getDetalles()) {
                    total = total.add(d.getSubtotal());
                }
                venta.setTotal(total);

                int ventaId = ventaDAO.insertarCabecera(conn, venta);
                for (DetalleVenta d : venta.getDetalles()) {
                    ventaDAO.insertarDetalle(conn, ventaId, d);
                    descontarStock(conn, d.getProductoId(), d.getCantidad());
                    movimientoDAO.insertar(conn, d.getProductoId(), TipoMovimiento.SALIDA,
                            d.getCantidad(), "Venta #" + ventaId, venta.getUsuarioId());
                }

                conn.commit();
                return ventaId;
            } catch (SQLException | IllegalStateException e) {
                conn.rollback();
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de conexión al registrar la venta", e);
        }
    }

    private void validarStockDisponible(Connection conn, Venta venta) throws SQLException {
        for (DetalleVenta d : venta.getDetalles()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT stock FROM productos WHERE id=?")) {
                ps.setInt(1, d.getProductoId());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("stock") < d.getCantidad()) {
                        throw new IllegalStateException(
                                "Stock insuficiente para " + d.getProductoNombre());
                    }
                }
            }
        }
    }

    private void descontarStock(Connection conn, int productoId, int cantidad) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE productos SET stock = stock - ? WHERE id = ?")) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        }
    }
}
