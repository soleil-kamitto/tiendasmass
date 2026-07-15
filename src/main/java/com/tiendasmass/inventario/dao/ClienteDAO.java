package com.tiendasmass.inventario.dao;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes ORDER BY nombre";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar clientes", e);
        }
        return clientes;
    }

    public void insertar(Cliente c) {
        String sql = "INSERT INTO clientes (nombre, documento) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getDocumento());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar cliente", e);
        }
    }

    public void eliminar(int id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM clientes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se puede eliminar: el cliente tiene ventas asociadas", e);
        }
    }

    private Cliente map(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("documento"));
    }
}
