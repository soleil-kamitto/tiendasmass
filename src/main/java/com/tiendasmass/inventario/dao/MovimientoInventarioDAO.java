package com.tiendasmass.inventario.dao;

import com.tiendasmass.inventario.model.MovimientoInventario;
import com.tiendasmass.inventario.model.TipoMovimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {

    /** Inserta el movimiento usando la conexión/transacción provista por el llamador. */
    public void insertar(Connection conn, int productoId, TipoMovimiento tipo, int cantidad,
                          String motivo, int usuarioId) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario (producto_id, tipo, cantidad, motivo, fecha, usuario_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, tipo.name());
            ps.setInt(3, cantidad);
            ps.setString(4, motivo);
            ps.setString(5, LocalDateTime.now().toString());
            ps.setInt(6, usuarioId);
            ps.executeUpdate();
        }
    }

    public List<MovimientoInventario> listarHistorial(Connection conn) throws SQLException {
        String sql = """
            SELECT m.*, p.nombre AS producto_nombre, u.nombre AS usuario_nombre
            FROM movimientos_inventario m
            JOIN productos p ON m.producto_id = p.id
            JOIN usuarios u ON m.usuario_id = u.id
            ORDER BY m.fecha DESC
        """;
        List<MovimientoInventario> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MovimientoInventario m = new MovimientoInventario();
                m.setId(rs.getInt("id"));
                m.setProductoId(rs.getInt("producto_id"));
                m.setProductoNombre(rs.getString("producto_nombre"));
                m.setTipo(TipoMovimiento.valueOf(rs.getString("tipo")));
                m.setCantidad(rs.getInt("cantidad"));
                m.setMotivo(rs.getString("motivo"));
                m.setFecha(LocalDateTime.parse(rs.getString("fecha")));
                m.setUsuarioNombre(rs.getString("usuario_nombre"));
                lista.add(m);
            }
        }
        return lista;
    }
}
