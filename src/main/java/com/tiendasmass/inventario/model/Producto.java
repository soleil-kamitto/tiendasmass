package com.tiendasmass.inventario.model;

import java.math.BigDecimal;

public class Producto {
    private int id;
    private String codigo;
    private String nombre;
    private String categoria;
    private BigDecimal precio;
    private int stock;
    private int stockMinimo;

    public Producto() {
    }

    public Producto(int id, String codigo, String nombre, String categoria,
                     BigDecimal precio, int stock, int stockMinimo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isStockBajo() {
        return stock < stockMinimo;
    }
}
