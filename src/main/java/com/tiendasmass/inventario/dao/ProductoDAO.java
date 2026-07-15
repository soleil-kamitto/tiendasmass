package com.tiendasmass.inventario.dao;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.model.Producto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public List<Producto> listarTodos() {
        String sql = "SELECT * FROM productos ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos", e);
        }
        return productos;
    }

    public List<Producto> buscar(String termino) {
        String sql = "SELECT * FROM productos WHERE nombre LIKE ? OR codigo LIKE ? ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        String like = "%" + termino + "%";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar productos", e);
        }
        return productos;
    }

    public List<Producto> listarStockBajo() {
        String sql = "SELECT * FROM productos WHERE stock < stock_minimo ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar stock bajo", e);
        }
        return productos;
    }

    public void insertar(Producto p) {
        String sql = "INSERT INTO productos (codigo, nombre, categoria, precio, stock, stock_minimo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getCategoria());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setInt(5, p.getStock());
            ps.setInt(6, p.getStockMinimo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar producto (¿código duplicado?)", e);
        }
    }

    public void actualizar(Producto p) {
        String sql = "UPDATE productos SET codigo=?, nombre=?, categoria=?, precio=?, stock_minimo=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getCategoria());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setInt(5, p.getStockMinimo());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar producto", e);
        }
    }

    public void eliminar(int id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM productos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se puede eliminar: el producto tiene movimientos o ventas asociadas", e);
        }
    }

    private Producto map(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                new BigDecimal(rs.getString("precio")),
                rs.getInt("stock"),
                rs.getInt("stock_minimo")
        );
    }
}
