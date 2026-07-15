package com.tiendasmass.inventario.model;

public class Cliente {
    private int id;
    private String nombre;
    private String documento;

    public Cliente() {
    }

    public Cliente(int id, String nombre, String documento) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    @Override
    public String toString() {
        return nombre;
    }
}
