package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.ClienteDAO;
import com.tiendasmass.inventario.model.Cliente;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ClientesPanel extends JPanel implements Refrescable {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{"ID", "Nombre", "Documento"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public ClientesPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.FONDO);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        barra.setBackground(Theme.FONDO);
        JButton nuevo = Theme.botonPrimario("+ Nuevo cliente");
        JButton borrar = Theme.botonPeligro("Borrar");
        nuevo.addActionListener(e -> nuevoCliente());
        borrar.addActionListener(e -> borrarCliente());
        barra.add(nuevo);
        barra.add(borrar);
        add(barra, BorderLayout.NORTH);

        Theme.estilizarTabla(tabla);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarDatos();
    }

    private void cargarDatos() {
        modelo.setRowCount(0);
        List<Cliente> clientes = clienteDAO.listarTodos();
        for (Cliente c : clientes) {
            modelo.addRow(new Object[]{c.getId(), c.getNombre(), c.getDocumento()});
        }
    }

    private void nuevoCliente() {
        JTextField nombre = new JTextField();
        JTextField documento = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBorder(new EmptyBorder(8, 0, 8, 0));
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Documento:"));
        panel.add(documento);

        int resultado = JOptionPane.showConfirmDialog(this, panel, "Nuevo cliente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION) {
            if (nombre.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Dato incompleto",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Cliente c = new Cliente();
            c.setNombre(nombre.getText().trim());
            c.setDocumento(documento.getText().trim());
            clienteDAO.insertar(c);
            cargarDatos();
        }
    }

    private void borrarCliente() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente.", "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) modelo.getValueAt(fila, 0);
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Eliminar este cliente?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                clienteDAO.eliminar(id);
                cargarDatos();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refrescar() {
        cargarDatos();
    }
}
