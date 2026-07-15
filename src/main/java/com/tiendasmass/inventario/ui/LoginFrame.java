package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.model.Usuario;
import com.tiendasmass.inventario.service.AuthService;
import com.tiendasmass.inventario.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();
    private final JTextField campoUsuario = new JTextField();
    private final JPasswordField campoPassword = new JPasswordField();

    public LoginFrame() {
        super("Sistema Tiendas Mass - Inicio de Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel raiz = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.AZUL_MASS_CLARO, 0, getHeight(), Theme.SIDEBAR_BG));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        TarjetaRedondeada centro = new TarjetaRedondeada(22, true);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBackground(Theme.BLANCO);
        centro.setBorder(new EmptyBorder(34, 40, 28, 40));
        centro.setPreferredSize(new Dimension(360, 460));

        JLabel logo = new JLabel(
                "<html><font face='Segoe UI' color='#142A7A'><i><b>Mass</b></i> &#10003;</font></html>",
                SwingConstants.CENTER);
        logo.setOpaque(true);
        logo.setBackground(Theme.AMARILLO);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setMaximumSize(new Dimension(160, 90));
        logo.setPreferredSize(new Dimension(160, 90));
        logo.setBorder(BorderFactory.createLineBorder(Theme.AMARILLO_OSCURO, 2));

        JLabel titulo = new JLabel("Sistema de Inventario y Ventas");
        titulo.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        titulo.setForeground(Theme.AZUL_MASS);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitulo = new JLabel("v1.0 — Tienda SJL");
        subtitulo.setFont(Theme.FUENTE_SUBTITULO);
        subtitulo.setForeground(Theme.TEXTO_SUAVE);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        centro.add(Box.createVerticalStrut(10));
        centro.add(logo);
        centro.add(Box.createVerticalStrut(16));
        centro.add(titulo);
        centro.add(subtitulo);
        centro.add(Box.createVerticalStrut(24));

        centro.add(campoConEtiqueta("Usuario:", campoUsuario));
        centro.add(Box.createVerticalStrut(12));
        centro.add(campoConEtiqueta("Contraseña:", campoPassword));
        centro.add(Box.createVerticalStrut(20));

        JButton ingresar = Theme.botonPrimario("INGRESAR");
        ingresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton cancelar = Theme.botonSecundario("Cancelar");
        cancelar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botones.setBackground(Theme.BLANCO);
        botones.add(ingresar);
        botones.add(cancelar);
        botones.setAlignmentX(Component.CENTER_ALIGNMENT);
        centro.add(botones);

        JLabel ayuda = new JLabel("Usuarios demo: admin / admin123 · empleado / empleado123");
        ayuda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ayuda.setForeground(Theme.TEXTO_SUAVE);
        ayuda.setAlignmentX(Component.CENTER_ALIGNMENT);
        centro.add(Box.createVerticalStrut(14));
        centro.add(ayuda);

        raiz.add(centro, new GridBagConstraints());
        setContentPane(raiz);

        getRootPane().setDefaultButton(ingresar);
        ingresar.addActionListener(e -> intentarLogin());
        cancelar.addActionListener(e -> System.exit(0));
    }

    private JPanel campoConEtiqueta(String etiqueta, JTextField campo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BLANCO);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(300, 55));

        JLabel label = new JLabel(etiqueta);
        label.setFont(Theme.FUENTE_NORMAL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setFont(Theme.FUENTE_NORMAL);
        campo.setMaximumSize(new Dimension(300, 34));
        campo.putClientProperty("JComponent.minimumWidth", 0);
        campo.setMargin(new Insets(4, 10, 4, 10));

        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        panel.add(campo);
        return panel;
    }

    private void intentarLogin() {
        String usuario = campoUsuario.getText().trim();
        String password = new String(campoPassword.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa usuario y contraseña.", "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Optional<Usuario> resultado = authService.login(usuario, password);
        if (resultado.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos, o cuenta inactiva.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            campoPassword.setText("");
            return;
        }

        Session.iniciar(resultado.get());
        dispose();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
