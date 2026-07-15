package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.MovimientoInventarioDAO;
import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.model.MovimientoInventario;
import com.tiendasmass.inventario.model.TipoMovimiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/** RF13/RF14: reposición de stock e historial de movimientos de inventario. */
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

    public void reponerStock(int productoId, int cantidad, String motivo, int usuarioId) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor a 0");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE productos SET stock = stock + ? WHERE id = ?")) {
                    ps.setInt(1, cantidad);
                    ps.setInt(2, productoId);
                    ps.executeUpdate();
                }
                movimientoDAO.insertar(conn, productoId, TipoMovimiento.ENTRADA, cantidad, motivo, usuarioId);
                conn.commit();
                log.info("Stock repuesto: producto={}, cantidad={}, usuario={}, motivo='{}'",
                        productoId, cantidad, usuarioId, motivo);
            } catch (SQLException e) {
                conn.rollback();
                log.error("Error al reponer stock (producto={}, usuario={})", productoId, usuarioId, e);
                throw new RuntimeException("Error al reponer stock", e);
            }
        } catch (SQLException e) {
            log.error("Error de conexión al reponer stock (producto={})", productoId, e);
            throw new RuntimeException("Error de conexión al reponer stock", e);
        }
    }

    public List<MovimientoInventario> historial() {
        try (Connection conn = Database.getConnection()) {
            return movimientoDAO.listarHistorial(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener historial de movimientos", e);
        }
    }
}
