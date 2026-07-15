package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.ClienteDAO;
import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.Cliente;
import com.tiendasmass.inventario.model.DetalleVenta;
import com.tiendasmass.inventario.model.Producto;
import com.tiendasmass.inventario.model.Venta;
import com.tiendasmass.inventario.service.VentaService;
import com.tiendasmass.inventario.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** RF04/RF05/RF15: registro de ventas con búsqueda de productos y actualización automática de stock. */
public class VentasPanel extends JPanel implements Refrescable {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VentaService ventaService = new VentaService();

    private final JComboBox<Cliente> comboCliente = new JComboBox<>();
    private final JTextField campoBusqueda = new JTextField();
    private final DefaultTableModel modeloResultados = new DefaultTableModel(
            new String[]{"Código", "Nombre", "Precio", "Stock"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tablaResultados = new JTable(modeloResultados);
    private final JSpinner spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

    private final DefaultTableModel modeloCarrito = new DefaultTableModel(
            new String[]{"Producto", "Cantidad", "Precio unit.", "Subtotal"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tablaCarrito = new JTable(modeloCarrito);
    private final JLabel etiquetaTotal = new JLabel("Total: S/ 0.00");

    private final List<DetalleVenta> carrito = new ArrayList<>();
    private List<Producto> productosEncontrados = new ArrayList<>();

    public VentasPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.FONDO);

        add(construirBarraCliente(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, construirPanelBusqueda(), construirPanelCarrito());
        split.setResizeWeight(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        cargarClientes();
        buscarProductos("");
    }

    private JComponent construirBarraCliente() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(Theme.FONDO);
        JLabel etiqueta = new JLabel("Cliente:");
        etiqueta.setFont(Theme.FUENTE_NORMAL);
        comboCliente.setPreferredSize(new Dimension(220, 30));
        panel.add(etiqueta);
        panel.add(comboCliente);
        return panel;
    }

    private JComponent construirPanelBusqueda() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.FONDO);
        panel.setBorder(new EmptyBorder(0, 0, 0, 10));

        JLabel titulo = new JLabel("Buscar producto");
        titulo.setFont(Theme.FUENTE_NEGRITA);

        campoBusqueda.setFont(Theme.FUENTE_NORMAL);
        campoBusqueda.setToolTipText("Buscar por código o nombre (RF15)");
        campoBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDE), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        campoBusqueda.addActionListener(e -> buscarProductos(campoBusqueda.getText().trim()));

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBackground(Theme.FONDO);
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(campoBusqueda, BorderLayout.SOUTH);
        norte.setBorder(new EmptyBorder(0, 0, 8, 0));

        Theme.estilizarTabla(tablaResultados);
        tablaResultados.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        sur.setBackground(Theme.FONDO);
        sur.add(new JLabel("Cantidad:"));
        sur.add(spinnerCantidad);
        JButton agregar = Theme.botonPrimario("Agregar al carrito");
        agregar.addActionListener(e -> agregarAlCarrito());
        sur.add(agregar);

        panel.add(norte, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaResultados), BorderLayout.CENTER);
        panel.add(sur, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent construirPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.FONDO);

        JLabel titulo = new JLabel("Carrito de venta");
        titulo.setFont(Theme.FUENTE_NEGRITA);

        Theme.estilizarTabla(tablaCarrito);

        JButton quitar = Theme.botonSecundario("Quitar seleccionado");
        quitar.addActionListener(e -> quitarDelCarrito());

        etiquetaTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton registrar = Theme.botonPrimario("Registrar venta");
        registrar.addActionListener(e -> registrarVenta());

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBackground(Theme.FONDO);
        inferior.add(etiquetaTotal, BorderLayout.WEST);
        inferior.add(registrar, BorderLayout.EAST);
        inferior.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(Theme.FONDO);
        sur.add(quitar, BorderLayout.NORTH);
        sur.add(inferior, BorderLayout.SOUTH);

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);
        panel.add(sur, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarClientes() {
        comboCliente.removeAllItems();
        for (Cliente c : clienteDAO.listarTodos()) {
            comboCliente.addItem(c);
        }
    }

    private void buscarProductos(String termino) {
        modeloResultados.setRowCount(0);
        productosEncontrados = termino.isBlank() ? productoDAO.listarTodos() : productoDAO.buscar(termino);
        for (Producto p : productosEncontrados) {
            modeloResultados.addRow(new Object[]{p.getCodigo(), p.getNombre(), "S/ " + p.getPrecio(), p.getStock()});
        }
    }

    private void agregarAlCarrito() {
        int fila = tablaResultados.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la lista.", "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Producto producto = productosEncontrados.get(fila);
        int cantidad = (Integer) spinnerCantidad.getValue();

        if (cantidad > producto.getStock()) {
            JOptionPane.showMessageDialog(this,
                    "Stock insuficiente. Disponible: " + producto.getStock(), "Stock insuficiente",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (DetalleVenta d : carrito) {
            if (d.getProductoId() == producto.getId()) {
                d.setCantidad(d.getCantidad() + cantidad);
                actualizarCarrito();
                return;
            }
        }
        carrito.add(new DetalleVenta(producto.getId(), producto.getNombre(), cantidad, producto.getPrecio()));
        actualizarCarrito();
    }

    private void quitarDelCarrito() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila < 0) return;
        carrito.remove(fila);
        actualizarCarrito();
    }

    private void actualizarCarrito() {
        modeloCarrito.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta d : carrito) {
            modeloCarrito.addRow(new Object[]{
                    d.getProductoNombre(), d.getCantidad(), "S/ " + d.getPrecioUnitario(), "S/ " + d.getSubtotal()
            });
            total = total.add(d.getSubtotal());
        }
        etiquetaTotal.setText("Total: S/ " + total.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private void registrarVenta() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un producto al carrito.", "Carrito vacío",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Cliente cliente = (Cliente) comboCliente.getSelectedItem();
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente.", "Falta cliente",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Venta venta = new Venta();
        venta.setClienteId(cliente.getId());
        venta.setUsuarioId(Session.actual().getId());
        venta.setDetalles(new ArrayList<>(carrito));

        try {
            int ventaId = ventaService.registrarVenta(venta);
            JOptionPane.showMessageDialog(this, "Venta #" + ventaId + " registrada correctamente.",
                    "Venta registrada", JOptionPane.INFORMATION_MESSAGE);
            carrito.clear();
            actualizarCarrito();
            buscarProductos(campoBusqueda.getText().trim());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo registrar la venta",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refrescar() {
        cargarClientes();
        buscarProductos(campoBusqueda.getText().trim());
    }
}
