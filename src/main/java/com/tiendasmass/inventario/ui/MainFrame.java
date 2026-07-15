package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contenido = new JPanel(cardLayout);
    private final JLabel tituloModulo = new JLabel();
    private final JLabel subtituloModulo = new JLabel();
    private final Map<String, JButton> botonesSidebar = new LinkedHashMap<>();
    private String moduloActivo = "";

    private static final String[][] MODULOS = {
            {"productos", "Productos", "Gestión de Productos", "Registro, búsqueda y actualización del catálogo"},
            {"inventario", "Inventario", "Control de Inventario", "Stock en tiempo real, reposición e historial de movimientos"},
            {"ventas", "Ventas", "Registro de Ventas", "Punto de venta con actualización automática de stock"},
            {"clientes", "Clientes", "Gestión de Clientes", "Registro de clientes de la tienda"},
            {"reclamos", "Reclamos", "Libro de Reclamos", "Registro y seguimiento de incidencias reportadas"},
            {"reportes", "Reportes", "Reportes", "Ventas por periodo y rotación de productos"},
            {"usuarios", "Usuarios", "Gestión de Usuarios", "Administración de cuentas, roles y permisos"},
    };

    public MainFrame() {
        super("Sistema Tiendas Mass — Inventario y Ventas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(950, 600));

        setJMenuBar(construirMenuBar());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Theme.FONDO);

        raiz.add(construirSidebar(), BorderLayout.WEST);
        raiz.add(construirAreaContenido(), BorderLayout.CENTER);
        raiz.add(construirBarraEstado(), BorderLayout.SOUTH);

        setContentPane(raiz);

        contenido.add(new ProductosPanel(), "productos");
        contenido.add(new InventarioPanel(), "inventario");
        contenido.add(new VentasPanel(), "ventas");
        contenido.add(new ClientesPanel(), "clientes");
        contenido.add(new ReclamosPanel(), "reclamos");
        contenido.add(new ReportesPanel(), "reportes");
        if (Session.esAdministrador()) {
            contenido.add(new UsuariosPanel(), "usuarios");
        }

        seleccionarModulo("productos");
    }

    private JMenuBar construirMenuBar() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem cerrarSesion = new JMenuItem("Cerrar sesión");
        cerrarSesion.addActionListener(e -> cerrarSesion());
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> System.exit(0));
        archivo.add(cerrarSesion);
        archivo.addSeparator();
        archivo.add(salir);

        JMenu modulos = new JMenu("Módulos");
        for (String[] m : MODULOS) {
            if (m[0].equals("usuarios") && !Session.esAdministrador()) continue;
            JMenuItem item = new JMenuItem(m[1]);
            item.addActionListener(e -> seleccionarModulo(m[0]));
            modulos.add(item);
        }

        JMenu ayuda = new JMenu("Ayuda");
        JMenuItem acercaDe = new JMenuItem("Acerca de");
        acercaDe.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Sistema de Inventario y Ventas para Tiendas Mass - SJL\nDesarrollado en Java (Swing + JDBC/SQLite)\nv1.0",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE));
        ayuda.add(acercaDe);

        barra.add(archivo);
        barra.add(modulos);
        barra.add(ayuda);
        return barra;
    }

    private JComponent construirSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel logo = new JLabel(
                "<html><font face='Segoe UI' color='#142A7A'><i><b>Mass</b></i> &#10003;</font></html>",
                SwingConstants.CENTER);
        logo.setOpaque(true);
        logo.setBackground(Theme.AMARILLO);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setMaximumSize(new Dimension(140, 42));
        logo.setPreferredSize(new Dimension(140, 42));

        JLabel subtitulo = new JLabel("Inventario y Ventas");
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subtitulo.setForeground(Theme.SIDEBAR_TEXTO);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(subtitulo);
        sidebar.add(Box.createVerticalStrut(20));

        JSeparator separador = new JSeparator();
        separador.setForeground(Theme.SIDEBAR_HOVER);
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(separador);
        sidebar.add(Box.createVerticalStrut(14));

        JLabel encabezado = new JLabel("MÓDULOS");
        encabezado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        encabezado.setForeground(Theme.SIDEBAR_TEXTO);
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.setBorder(new EmptyBorder(0, 20, 8, 16));
        sidebar.add(encabezado);

        for (String[] m : MODULOS) {
            if (m[0].equals("usuarios") && !Session.esAdministrador()) continue;
            JButton boton = new JButton(m[1]);
            boton.setHorizontalAlignment(SwingConstants.LEFT);
            boton.setFont(Theme.FUENTE_NORMAL);
            boton.setFocusPainted(false);
            boton.setBorder(new EmptyBorder(10, 20, 10, 16));
            boton.setBackground(Theme.SIDEBAR_BG);
            boton.setForeground(Theme.SIDEBAR_TEXTO);
            boton.setAlignmentX(Component.LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            boton.setBorderPainted(false);
            boton.setOpaque(true);
            boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boton.getModel().addChangeListener(e -> {
                if (m[0].equals(moduloActivo)) return;
                boton.setBackground(boton.getModel().isRollover() ? Theme.SIDEBAR_HOVER : Theme.SIDEBAR_BG);
            });
            boton.addActionListener(e -> seleccionarModulo(m[0]));
            botonesSidebar.put(m[0], boton);
            sidebar.add(boton);
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JComponent construirAreaContenido() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.FONDO);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBackground(Theme.FONDO);
        tituloModulo.setFont(Theme.FUENTE_TITULO);
        tituloModulo.setForeground(Theme.AZUL_MASS);
        subtituloModulo.setFont(Theme.FUENTE_SUBTITULO);
        subtituloModulo.setForeground(Theme.TEXTO_SUAVE);
        encabezado.add(tituloModulo);
        encabezado.add(subtituloModulo);
        encabezado.setBorder(new EmptyBorder(0, 0, 16, 0));

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }

    private JComponent construirBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Theme.BLANCO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDE),
                new EmptyBorder(6, 16, 6, 16)));

        JLabel izquierda = new JLabel("Usuario: " + Session.actual().getNombre()
                + "  ·  Rol: " + Session.actual().getRol() + "  ·  Tienda: SJL  ·  v1.0");
        izquierda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        izquierda.setForeground(Theme.TEXTO_SUAVE);

        JLabel derecha = new JLabel("● Conectado a BD (SQLite)");
        derecha.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        derecha.setForeground(Theme.VERDE);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private void seleccionarModulo(String id) {
        moduloActivo = id;
        for (String[] m : MODULOS) {
            if (m[0].equals(id)) {
                tituloModulo.setText(m[2]);
                subtituloModulo.setText(m[3]);
                break;
            }
        }
        botonesSidebar.forEach((clave, boton) -> {
            boolean seleccionado = clave.equals(id);
            boton.setBackground(seleccionado ? Theme.AMARILLO : Theme.SIDEBAR_BG);
            boton.setForeground(seleccionado ? Theme.AZUL_MASS : Theme.SIDEBAR_TEXTO);
            boton.setFont(seleccionado ? Theme.FUENTE_NEGRITA : Theme.FUENTE_NORMAL);
        });
        cardLayout.show(contenido, id);

        Component actual = obtenerPanelVisible(id);
        if (actual instanceof Refrescable r) {
            r.refrescar();
        }
    }

    private Component obtenerPanelVisible(String id) {
        int idx = 0;
        for (String[] m : MODULOS) {
            if (m[0].equals("usuarios") && !Session.esAdministrador()) continue;
            if (m[0].equals(id)) break;
            idx++;
        }
        return contenido.getComponent(idx);
    }

    private void cerrarSesion() {
        Session.cerrar();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
