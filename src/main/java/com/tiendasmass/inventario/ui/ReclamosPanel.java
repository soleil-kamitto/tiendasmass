package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.ClienteDAO;
import com.tiendasmass.inventario.dao.ReclamoDAO;
import com.tiendasmass.inventario.model.Cliente;
import com.tiendasmass.inventario.model.Reclamo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReclamosPanel extends JPanel implements Refrescable {

    private final ReclamoDAO reclamoDAO = new ReclamoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] ESTADOS = {"PENDIENTE", "EN_PROCESO", "RESUELTO"};

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Fecha", "Cliente", "Descripción", "Estado"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public ReclamosPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.FONDO);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        barra.setBackground(Theme.FONDO);
        JButton nuevo = Theme.botonPrimario("+ Nuevo reclamo");
        JButton cambiarEstado = Theme.botonSecundario("Cambiar estado");
        nuevo.addActionListener(e -> nuevoReclamo());
        cambiarEstado.addActionListener(e -> cambiarEstado());
        barra.add(nuevo);
        barra.add(cambiarEstado);
        add(barra, BorderLayout.NORTH);

        Theme.estilizarTabla(tabla);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarDatos();
    }

    private void cargarDatos() {
        modelo.setRowCount(0);
        List<Reclamo> reclamos = reclamoDAO.listarTodos();
        for (Reclamo r : reclamos) {
            modelo.addRow(new Object[]{
                    r.getId(), r.getFecha().format(FORMATO_FECHA), r.getClienteNombre(),
                    r.getDescripcion(), r.getEstado()
            });
        }
    }

    private void nuevoReclamo() {
        List<Cliente> clientes = clienteDAO.listarTodos();
        JComboBox<Cliente> comboCliente = new JComboBox<>(clientes.toArray(new Cliente[0]));
        JTextArea descripcion = new JTextArea(4, 20);
        descripcion.setLineWrap(true);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(8, 0, 8, 0));
        JPanel norte = new JPanel(new BorderLayout(8, 0));
        norte.add(new JLabel("Cliente:"), BorderLayout.WEST);
        norte.add(comboCliente, BorderLayout.CENTER);
        panel.add(norte, BorderLayout.NORTH);
        panel.add(new JScrollPane(descripcion), BorderLayout.CENTER);

        int resultado = JOptionPane.showConfirmDialog(this, panel, "Nuevo reclamo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION) {
            Cliente cliente = (Cliente) comboCliente.getSelectedItem();
            if (cliente == null || descripcion.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Selecciona un cliente y describe el reclamo.",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            reclamoDAO.insertar(cliente.getId(), descripcion.getText().trim());
            cargarDatos();
        }
    }

    private void cambiarEstado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un reclamo.", "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) modelo.getValueAt(fila, 0);
        String estadoActual = (String) modelo.getValueAt(fila, 4);
        String nuevoEstado = (String) JOptionPane.showInputDialog(this, "Nuevo estado:", "Cambiar estado",
                JOptionPane.PLAIN_MESSAGE, null, ESTADOS, estadoActual);
        if (nuevoEstado != null) {
            reclamoDAO.actualizarEstado(id, nuevoEstado);
            cargarDatos();
        }
    }

    @Override
    public void refrescar() {
        cargarDatos();
    }
}
