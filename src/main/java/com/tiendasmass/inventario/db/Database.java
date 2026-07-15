package com.tiendasmass.inventario.db;

import com.tiendasmass.inventario.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Acceso a la base de datos SQLite embebida (vía JDBC) y creación del esquema inicial. */
public final class Database {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:tiendas_mass.db";

    private static String dbUrl = DEFAULT_DB_URL;

    private Database() {
    }

    /** Permite redirigir la conexión a otra base (usado por los tests para no tocar tiendas_mass.db). */
    public static void setDbUrl(String url) {
        dbUrl = url;
    }

    public static void resetDbUrl() {
        dbUrl = DEFAULT_DB_URL;
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static void initSchema() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    usuario TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    rol TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS productos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    codigo TEXT NOT NULL UNIQUE,
                    nombre TEXT NOT NULL,
                    categoria TEXT,
                    precio REAL NOT NULL,
                    stock INTEGER NOT NULL DEFAULT 0,
                    stock_minimo INTEGER NOT NULL DEFAULT 5
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    documento TEXT
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ventas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha TEXT NOT NULL,
                    cliente_id INTEGER NOT NULL REFERENCES clientes(id),
                    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
                    total REAL NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS detalle_venta (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venta_id INTEGER NOT NULL REFERENCES ventas(id),
                    producto_id INTEGER NOT NULL REFERENCES productos(id),
                    cantidad INTEGER NOT NULL,
                    precio_unitario REAL NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reclamos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cliente_id INTEGER NOT NULL REFERENCES clientes(id),
                    descripcion TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                    fecha TEXT NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS movimientos_inventario (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    producto_id INTEGER NOT NULL REFERENCES productos(id),
                    tipo TEXT NOT NULL,
                    cantidad INTEGER NOT NULL,
                    motivo TEXT,
                    fecha TEXT NOT NULL,
                    usuario_id INTEGER NOT NULL REFERENCES usuarios(id)
                )
            """);

            seedData(conn);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    private static void seedData(Connection conn) throws SQLException {
        try (Statement check = conn.createStatement()) {
            var rs = check.executeQuery("SELECT COUNT(*) AS c FROM usuarios");
            rs.next();
            if (rs.getInt("c") > 0) {
                return; // ya inicializado
            }
        }

        try (var ps = conn.prepareStatement(
                "INSERT INTO usuarios (nombre, usuario, password_hash, rol, activo) VALUES (?, ?, ?, ?, 1)")) {
            ps.setString(1, "Administrador");
            ps.setString(2, "admin");
            ps.setString(3, PasswordUtil.hash("admin123"));
            ps.setString(4, "ADMINISTRADOR");
            ps.executeUpdate();

            ps.setString(1, "Empleado Demo");
            ps.setString(2, "empleado");
            ps.setString(3, PasswordUtil.hash("empleado123"));
            ps.setString(4, "EMPLEADO");
            ps.executeUpdate();
        }

        try (var ps = conn.prepareStatement("INSERT INTO clientes (nombre, documento) VALUES (?, ?)")) {
            ps.setString(1, "Cliente General");
            ps.setString(2, "00000000");
            ps.executeUpdate();
        }

        try (var ps = conn.prepareStatement(
                "INSERT INTO productos (codigo, nombre, categoria, precio, stock, stock_minimo) VALUES (?, ?, ?, ?, ?, ?)")) {
            Object[][] demo = {
                {"P001", "Arroz Costeño 1kg", "Abarrotes", 4.50, 40, 10},
                {"P002", "Aceite Primor 1L", "Abarrotes", 12.90, 25, 8},
                {"P003", "Detergente Bolivar 500g", "Limpieza", 6.20, 30, 10},
                {"P004", "Gaseosa Kola Real 1.5L", "Bebidas", 4.00, 6, 12},
                {"P005", "Fideos Don Vittorio 500g", "Abarrotes", 3.50, 50, 15},
                {"P006", "Leche Gloria Evap. 400g", "Lácteos", 3.80, 3, 10},
            };
            for (Object[] p : demo) {
                ps.setString(1, (String) p[0]);
                ps.setString(2, (String) p[1]);
                ps.setString(3, (String) p[2]);
                ps.setDouble(4, (Double) p[3]);
                ps.setInt(5, (Integer) p[4]);
                ps.setInt(6, (Integer) p[5]);
                ps.executeUpdate();
            }
        }
    }
}
