package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.MovimientoInventario;
import com.tiendasmass.inventario.model.Producto;
import com.tiendasmass.inventario.service.InventarioService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class InventarioPanel extends JPanel implements Refrescable {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final InventarioService inventarioService = new InventarioService();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DefaultTableModel modeloStock = new DefaultTableModel(
            new String[]{"Código", "Nombre", "Categoría", "Stock", "Stock mínimo", "Estado"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel modeloHistorial = new DefaultTableModel(
            new String[]{"Fecha", "Producto", "Tipo", "Cantidad", "Motivo", "Usuario"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel etiquetaAlertas = new JLabel();

    public InventarioPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.FONDO);

        add(construirBarraSuperior(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FUENTE_NORMAL);

        JTable tablaStock = new JTable(modeloStock);
        estiloTabla(tablaStock);
        tablaStock.getColumnModel().getColumn(5).setCellRenderer(new EstadoStockRenderer());
        tabs.addTab("Stock actual (tiempo real)", new JScrollPane(tablaStock));

        JTable tablaHistorial = new JTable(modeloHistorial);
        estiloTabla(tablaHistorial);
        tabs.addTab("Historial de movimientos", new JScrollPane(tablaHistorial));

        add(tabs, BorderLayout.CENTER);

        etiquetaAlertas.setFont(Theme.FUENTE_NEGRITA);
        etiquetaAlertas.setForeground(Theme.NARANJA);
        add(etiquetaAlertas, BorderLayout.SOUTH);

        cargarDatos();
    }

    private void estiloTabla(JTable tabla) {
        Theme.estilizarTabla(tabla);
    }

    private JComponent construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Theme.FONDO);
        JLabel info = new JLabel("El stock se actualiza automáticamente con cada venta o reposición.");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(Theme.TEXTO_SUAVE);

        JButton reponer = Theme.botonPrimario("+ Reponer stock");
        reponer.addActionListener(e -> abrirReposicion());

        barra.add(info, BorderLayout.WEST);
        barra.add(reponer, BorderLayout.EAST);
        return barra;
    }

    private void abrirReposicion() {
        ReponerStockDialog dialogo = new ReponerStockDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            cargarDatos();
        }
    }

    private void cargarDatos() {
        modeloStock.setRowCount(0);
        int stockBajo = 0;
        for (Producto p : productoDAO.listarTodos()) {
            modeloStock.addRow(new Object[]{
                    p.getCodigo(), p.getNombre(), p.getCategoria(), p.getStock(), p.getStockMinimo(),
                    Theme.estadoStock(p.getStock(), p.getStockMinimo())
            });
            if (p.isStockBajo()) stockBajo++;
        }
        etiquetaAlertas.setText(stockBajo == 0
                ? "Sin alertas de stock bajo."
                : "⚠ " + stockBajo + " producto(s) con stock por debajo del mínimo definido.");
        etiquetaAlertas.setForeground(stockBajo == 0 ? Theme.VERDE : Theme.NARANJA);

        modeloHistorial.setRowCount(0);
        for (MovimientoInventario m : inventarioService.historial()) {
            modeloHistorial.addRow(new Object[]{
                    m.getFecha().format(FORMATO_FECHA), m.getProductoNombre(), m.getTipo(),
                    m.getCantidad(), m.getMotivo(), m.getUsuarioNombre()
            });
        }
    }

    @Override
    public void refrescar() {
        cargarDatos();
    }
}
