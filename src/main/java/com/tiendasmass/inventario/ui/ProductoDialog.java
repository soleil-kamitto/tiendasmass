package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.model.Producto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/** Formulario modal para registrar (RF01) o actualizar (RF02) un producto. */
public class ProductoDialog extends JDialog {

    private final JTextField campoCodigo = new JTextField();
    private final JTextField campoNombre = new JTextField();
    private final JTextField campoCategoria = new JTextField();
    private final JTextField campoPrecio = new JTextField();
    private final JTextField campoStock = new JTextField();
    private final JTextField campoStockMinimo = new JTextField();

    private boolean confirmado = false;
    private final boolean esNuevo;

    public ProductoDialog(Frame owner, Producto producto) {
        super(owner, producto == null ? "Nuevo producto" : "Editar producto", true);
        this.esNuevo = producto == null;
        setSize(380, 400);
        setLocationRelativeTo(owner);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Theme.BLANCO);

        panel.add(fila("Código:", campoCodigo));
        panel.add(fila("Nombre:", campoNombre));
        panel.add(fila("Categoría:", campoCategoria));
        panel.add(fila("Precio (S/):", campoPrecio));
        panel.add(fila("Stock inicial:", campoStock));
        panel.add(fila("Stock mínimo (alerta):", campoStockMinimo));

        if (!esNuevo) {
            campoStock.setEnabled(false);
            campoStock.setToolTipText("El stock solo se modifica desde Inventario (reposición) o mediante ventas");
        }

        if (producto != null) {
            campoCodigo.setText(producto.getCodigo());
            campoNombre.setText(producto.getNombre());
            campoCategoria.setText(producto.getCategoria());
            campoPrecio.setText(producto.getPrecio().toPlainString());
            campoStock.setText(String.valueOf(producto.getStock()));
            campoStockMinimo.setText(String.valueOf(producto.getStockMinimo()));
        } else {
            campoStock.setText("0");
            campoStockMinimo.setText("5");
        }

        JButton guardar = Theme.botonPrimario("Guardar");
        JButton cancelar = Theme.botonSecundario("Cancelar");
        guardar.addActionListener(e -> guardar());
        cancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setBackground(Theme.BLANCO);
        botones.add(cancelar);
        botones.add(guardar);

        panel.add(Box.createVerticalStrut(10));
        panel.add(botones);

        setContentPane(panel);
    }

    private JPanel fila(String etiqueta, JTextField campo) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setBackground(Theme.BLANCO);
        fila.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel label = new JLabel(etiqueta);
        label.setFont(Theme.FUENTE_NORMAL);
        label.setPreferredSize(new Dimension(140, 24));
        campo.setFont(Theme.FUENTE_NORMAL);
        fila.add(label, BorderLayout.WEST);
        fila.add(campo, BorderLayout.CENTER);
        return fila;
    }

    private void guardar() {
        if (campoCodigo.getText().isBlank() || campoNombre.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Código y nombre son obligatorios.", "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            new BigDecimal(campoPrecio.getText().trim());
            Integer.parseInt(campoStock.getText().trim());
            Integer.parseInt(campoStockMinimo.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Precio, stock y stock mínimo deben ser numéricos.",
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void volcarEn(Producto p) {
        p.setCodigo(campoCodigo.getText().trim());
        p.setNombre(campoNombre.getText().trim());
        p.setCategoria(campoCategoria.getText().trim());
        p.setPrecio(new BigDecimal(campoPrecio.getText().trim()));
        p.setStockMinimo(Integer.parseInt(campoStockMinimo.getText().trim()));
        if (esNuevo) {
            p.setStock(Integer.parseInt(campoStock.getText().trim()));
        }
    }
}
