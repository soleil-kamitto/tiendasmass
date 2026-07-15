package com.tiendasmass.inventario.dao;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.model.Rol;
import com.tiendasmass.inventario.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAO {

    public Optional<Usuario> buscarPorUsuario(String usuario) {
        String sql = "SELECT * FROM usuarios WHERE usuario = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario", e);
        }
        return Optional.empty();
    }

    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nombre";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar usuarios", e);
        }
        return usuarios;
    }

    public void insertar(Usuario u) {
        String sql = "INSERT INTO usuarios (nombre, usuario, password_hash, rol, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getUsuario());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRol().name());
            ps.setInt(5, u.isActivo() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar usuario (¿nombre de usuario duplicado?)", e);
        }
    }

    public void actualizarRolYEstado(int id, Rol rol, boolean activo) {
        String sql = "UPDATE usuarios SET rol=?, activo=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rol.name());
            ps.setInt(2, activo ? 1 : 0);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar usuario", e);
        }
    }

    public void cambiarPassword(int id, String nuevoHash) {
        String sql = "UPDATE usuarios SET password_hash=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar contraseña", e);
        }
    }

    private Usuario map(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("usuario"),
                rs.getString("password_hash"),
                Rol.valueOf(rs.getString("rol")),
                rs.getInt("activo") == 1
        );
    }
}
