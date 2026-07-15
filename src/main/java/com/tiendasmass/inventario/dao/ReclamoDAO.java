package com.tiendasmass.inventario.dao;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.model.Reclamo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReclamoDAO {

    public List<Reclamo> listarTodos() {
        String sql = """
            SELECT r.*, c.nombre AS cliente_nombre
            FROM reclamos r JOIN clientes c ON r.cliente_id = c.id
            ORDER BY r.fecha DESC
        """;
        List<Reclamo> reclamos = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reclamos.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar reclamos", e);
        }
        return reclamos;
    }

    public void insertar(int clienteId, String descripcion) {
        String sql = "INSERT INTO reclamos (cliente_id, descripcion, estado, fecha) VALUES (?, ?, 'PENDIENTE', ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.setString(2, descripcion);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar reclamo", e);
        }
    }

    public void actualizarEstado(int id, String estado) {
        String sql = "UPDATE reclamos SET estado=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar reclamo", e);
        }
    }

    private Reclamo map(ResultSet rs) throws SQLException {
        Reclamo r = new Reclamo();
        r.setId(rs.getInt("id"));
        r.setClienteId(rs.getInt("cliente_id"));
        r.setClienteNombre(rs.getString("cliente_nombre"));
        r.setDescripcion(rs.getString("descripcion"));
        r.setEstado(rs.getString("estado"));
        r.setFecha(LocalDateTime.parse(rs.getString("fecha")));
        return r;
    }
}
