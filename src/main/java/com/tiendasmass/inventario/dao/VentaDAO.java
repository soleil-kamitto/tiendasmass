package com.tiendasmass.inventario.dao;

import com.tiendasmass.inventario.model.DetalleVenta;
import com.tiendasmass.inventario.model.Venta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    /** Inserta la cabecera de venta usando la conexión/transacción del llamador y devuelve el id generado. */
    public int insertarCabecera(Connection conn, Venta venta) throws SQLException {
        String sql = "INSERT INTO ventas (fecha, cliente_id, usuario_id, total) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, venta.getClienteId());
            ps.setInt(3, venta.getUsuarioId());
            ps.setBigDecimal(4, venta.getTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void insertarDetalle(Connection conn, int ventaId, DetalleVenta d) throws SQLException {
        String sql = "INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ps.setInt(2, d.getProductoId());
            ps.setInt(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.executeUpdate();
        }
    }

    public List<Venta> listarEntre(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
            SELECT v.*, c.nombre AS cliente_nombre, u.nombre AS usuario_nombre
            FROM ventas v
            JOIN clientes c ON v.cliente_id = c.id
            JOIN usuarios u ON v.usuario_id = u.id
            WHERE v.fecha BETWEEN ? AND ?
            ORDER BY v.fecha DESC
        """;
        List<Venta> ventas = new ArrayList<>();
        try (Connection conn = com.tiendasmass.inventario.db.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venta v = new Venta();
                    v.setId(rs.getInt("id"));
                    v.setFecha(LocalDateTime.parse(rs.getString("fecha")));
                    v.setClienteId(rs.getInt("cliente_id"));
                    v.setClienteNombre(rs.getString("cliente_nombre"));
                    v.setUsuarioId(rs.getInt("usuario_id"));
                    v.setUsuarioNombre(rs.getString("usuario_nombre"));
                    v.setTotal(new BigDecimal(rs.getString("total")));
                    ventas.add(v);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar ventas", e);
        }
        return ventas;
    }

    public List<Object[]> rotacionProductos(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
            SELECT p.nombre, SUM(d.cantidad) AS unidades, SUM(d.cantidad * d.precio_unitario) AS total
            FROM detalle_venta d
            JOIN productos p ON d.producto_id = p.id
            JOIN ventas v ON d.venta_id = v.id
            WHERE v.fecha BETWEEN ? AND ?
            GROUP BY p.id
            ORDER BY unidades DESC
        """;
        List<Object[]> filas = new ArrayList<>();
        try (Connection conn = com.tiendasmass.inventario.db.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{rs.getString("nombre"), rs.getInt("unidades"), rs.getBigDecimal("total")});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular rotación de productos", e);
        }
        return filas;
    }
}
