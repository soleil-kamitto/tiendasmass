package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.UsuarioDAO;
import com.tiendasmass.inventario.model.Rol;
import com.tiendasmass.inventario.model.Usuario;
import com.tiendasmass.inventario.util.PasswordUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** RF10/RF12/RNF02: registro y gestión de usuarios, asignación de roles y permisos. Solo administradores. */
public class UsuariosPanel extends JPanel implements Refrescable {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Usuario", "Rol", "Activo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public UsuariosPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.FONDO);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        barra.setBackground(Theme.FONDO);
        JButton nuevo = Theme.botonPrimario("+ Nuevo usuario");
        JButton cambiarRol = Theme.botonSecundario("Rol / Activo");
        JButton resetPassword = Theme.botonSecundario("Restablecer contraseña");
        nuevo.addActionListener(e -> nuevoUsuario());
        cambiarRol.addActionListener(e -> cambiarRolActivo());
        resetPassword.addActionListener(e -> restablecerPassword());
        barra.add(nuevo);
        barra.add(cambiarRol);
        barra.add(resetPassword);
        add(barra, BorderLayout.NORTH);

        Theme.estilizarTabla(tabla);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarDatos();
    }

    private void cargarDatos() {
        modelo.setRowCount(0);
        List<Usuario> usuarios = usuarioDAO.listarTodos();
        for (Usuario u : usuarios) {
            modelo.addRow(new Object[]{u.getId(), u.getNombre(), u.getUsuario(), u.getRol(),
                    u.isActivo() ? "Sí" : "No"});
        }
    }

    private void nuevoUsuario() {
        JTextField nombre = new JTextField();
        JTextField usuario = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<Rol> rol = new JComboBox<>(Rol.values());

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.setBorder(new EmptyBorder(8, 0, 8, 0));
        panel.add(new JLabel("Nombre completo:"));
        panel.add(nombre);
        panel.add(new JLabel("Usuario (login):"));
        panel.add(usuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(password);
        panel.add(new JLabel("Rol:"));
        panel.add(rol);

        int resultado = JOptionPane.showConfirmDialog(this, panel, "Nuevo usuario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION) {
            if (nombre.getText().isBlank() || usuario.getText().isBlank() || password.getPassword().length == 0) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Datos incompletos",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Usuario u = new Usuario();
            u.setNombre(nombre.getText().trim());
            u.setUsuario(usuario.getText().trim());
            u.setPasswordHash(PasswordUtil.hash(new String(password.getPassword())));
            u.setRol((Rol) rol.getSelectedItem());
            u.setActivo(true);
            try {
                usuarioDAO.insertar(u);
                cargarDatos();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cambiarRolActivo() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.", "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) modelo.getValueAt(fila, 0);
        Rol rolActual = Rol.valueOf(String.valueOf(modelo.getValueAt(fila, 3)));
        boolean activoActual = "Sí".equals(modelo.getValueAt(fila, 4));

        JComboBox<Rol> rol = new JComboBox<>(Rol.values());
        rol.setSelectedItem(rolActual);
        JCheckBox activo = new JCheckBox("Activo", activoActual);

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Rol:"));
        panel.add(rol);
        panel.add(new JLabel("Estado:"));
        panel.add(activo);

        int resultado = JOptionPane.showConfirmDialog(this, panel, "Rol y estado de la cuenta",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION) {
            usuarioDAO.actualizarRolYEstado(id, (Rol) rol.getSelectedItem(), activo.isSelected());
            cargarDatos();
        }
    }

    private void restablecerPassword() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.", "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) modelo.getValueAt(fila, 0);
        JPasswordField password = new JPasswordField();
        int resultado = JOptionPane.showConfirmDialog(this, password, "Nueva contraseña",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION && password.getPassword().length > 0) {
            usuarioDAO.cambiarPassword(id, PasswordUtil.hash(new String(password.getPassword())));
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.", "Listo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void refrescar() {
        cargarDatos();
    }
}
