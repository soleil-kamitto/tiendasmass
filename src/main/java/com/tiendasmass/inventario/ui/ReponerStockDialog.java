package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.Producto;
import com.tiendasmass.inventario.service.InventarioService;
import com.tiendasmass.inventario.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** RF14: reposición de productos en el inventario. */
public class ReponerStockDialog extends JDialog {

    private final JComboBox<Producto> comboProducto;
    private final JTextField campoCantidad = new JTextField();
    private final JTextField campoMotivo = new JTextField("Reposición de stock");
    private boolean confirmado = false;

    public ReponerStockDialog(Frame owner) {
        super(owner, "Reponer stock", true);
        setSize(380, 300);
        setLocationRelativeTo(owner);

        java.util.List<Producto> productos = new ProductoDAO().listarTodos();
        comboProducto = new JComboBox<>(productos.toArray(new Producto[0]));
        comboProducto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Producto p) {
                    setText(p.getCodigo() + " - " + p.getNombre() + " (stock actual: " + p.getStock() + ")");
                }
                return c;
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Theme.BLANCO);

        panel.add(fila("Producto:", comboProducto));
        panel.add(fila("Cantidad a ingresar:", campoCantidad));
        panel.add(fila("Motivo:", campoMotivo));

        JButton guardar = Theme.botonPrimario("Registrar entrada");
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

    private JPanel fila(String etiqueta, JComponent campo) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setBackground(Theme.BLANCO);
        fila.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel label = new JLabel(etiqueta);
        label.setFont(Theme.FUENTE_NORMAL);
        label.setPreferredSize(new Dimension(140, 24));
        fila.add(label, BorderLayout.WEST);
        fila.add(campo, BorderLayout.CENTER);
        return fila;
    }

    private void guardar() {
        Producto seleccionado = (Producto) comboProducto.getSelectedItem();
        if (seleccionado == null) return;
        int cantidad;
        try {
            cantidad = Integer.parseInt(campoCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número entero mayor a 0.",
                    "Dato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            new InventarioService().reponerStock(seleccionado.getId(), cantidad,
                    campoMotivo.getText().trim(), Session.actual().getId());
            confirmado = true;
            dispose();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}
