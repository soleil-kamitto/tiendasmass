package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.Producto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProductosPanel extends JPanel implements Refrescable {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JTextField campoBusqueda = new JTextField();
    private final JLabel etiquetaTotal = new JLabel();

    private static final String[] COLUMNAS = {"Código", "Nombre", "Categoría", "Precio", "Stock", "Estado"};

    public ProductosPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.FONDO);

        add(construirBarraSuperior(), BorderLayout.NORTH);

        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        Theme.estilizarTabla(tabla);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new EstadoStockRenderer());
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDE));
        scroll.getViewport().setBackground(Theme.BLANCO);
        add(scroll, BorderLayout.CENTER);

        etiquetaTotal.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        etiquetaTotal.setForeground(Theme.TEXTO_SUAVE);
        add(etiquetaTotal, BorderLayout.SOUTH);

        cargarDatos(null);
    }

    private JComponent construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setBackground(Theme.FONDO);

        campoBusqueda.setFont(Theme.FUENTE_NORMAL);
        campoBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        campoBusqueda.addActionListener(e -> cargarDatos(campoBusqueda.getText().trim()));

        JPanel textoPanel = new JPanel(new BorderLayout());
        textoPanel.setBackground(Theme.FONDO);
        JTextField placeholder = campoBusqueda;
        placeholder.setToolTipText("Buscar por código o nombre… (Enter para buscar)");
        textoPanel.add(campoBusqueda, BorderLayout.CENTER);

        JButton nuevo = Theme.botonPrimario("+ Nuevo");
        JButton editar = Theme.botonSecundario("Editar");
        JButton borrar = Theme.botonPeligro("Borrar");

        nuevo.addActionListener(e -> nuevoProducto());
        editar.addActionListener(e -> editarProducto());
        borrar.addActionListener(e -> borrarProducto());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setBackground(Theme.FONDO);
        botones.add(nuevo);
        botones.add(editar);
        botones.add(borrar);

        barra.add(textoPanel, BorderLayout.CENTER);
        barra.add(botones, BorderLayout.EAST);
        return barra;
    }

    private void cargarDatos(String termino) {
        modelo.setRowCount(0);
        List<Producto> productos = (termino == null || termino.isBlank())
                ? productoDAO.listarTodos()
                : productoDAO.buscar(termino);
        for (Producto p : productos) {
            modelo.addRow(new Object[]{
                    p.getCodigo(), p.getNombre(), p.getCategoria(),
                    "S/ " + p.getPrecio().toPlainString(), p.getStock(),
                    Theme.estadoStock(p.getStock(), p.getStockMinimo())
            });
        }
        etiquetaTotal.setText("Total: " + productos.size() + " productos");
    }

    private Producto obtenerSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la tabla.", "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String codigo = (String) modelo.getValueAt(fila, 0);
        return productoDAO.buscar(codigo).stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst()
                .orElse(null);
    }

    private void nuevoProducto() {
        ProductoDialog dialogo = new ProductoDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            Producto p = new Producto();
            dialogo.volcarEn(p);
            try {
                productoDAO.insertar(p);
                cargarDatos(null);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editarProducto() {
        Producto seleccionado = obtenerSeleccionado();
        if (seleccionado == null) return;
        ProductoDialog dialogo = new ProductoDialog((Frame) SwingUtilities.getWindowAncestor(this), seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            dialogo.volcarEn(seleccionado);
            try {
                productoDAO.actualizar(seleccionado);
                cargarDatos(null);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void borrarProducto() {
        Producto seleccionado = obtenerSeleccionado();
        if (seleccionado == null) return;
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el producto \"" + seleccionado.getNombre() + "\"? Esta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                productoDAO.eliminar(seleccionado.getId());
                cargarDatos(null);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refrescar() {
        cargarDatos(campoBusqueda.getText().trim());
    }
}
