package com.tiendasmass.inventario.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

/** Paleta e insumos visuales compartidos, inspirados en la identidad de Tiendas Mass (amarillo + azul marino). */
public final class Theme {

    public static final Color AMARILLO = new Color(0xFF, 0xD1, 0x00);
    public static final Color AMARILLO_OSCURO = new Color(0xE6, 0xB8, 0x00);
    public static final Color AZUL_MASS = new Color(0x14, 0x2A, 0x7A);
    public static final Color AZUL_MASS_CLARO = new Color(0x2C, 0x45, 0x9E);

    // Sidebar de navegación (azul marino oscuro para un look premium en vez del gris plano por defecto)
    public static final Color SIDEBAR_BG = new Color(0x0E, 0x1F, 0x5C);
    public static final Color SIDEBAR_HOVER = new Color(0x1B, 0x33, 0x86);
    public static final Color SIDEBAR_TEXTO = new Color(0xC7, 0xD0, 0xEC);
    public static final Color SIDEBAR_TEXTO_ACTIVO = AZUL_MASS;

    public static final Color FONDO = new Color(0xEE, 0xF0, 0xF5);
    public static final Color BLANCO = Color.WHITE;
    public static final Color TEXTO = new Color(0x20, 0x22, 0x2B);
    public static final Color TEXTO_SUAVE = new Color(0x74, 0x78, 0x87);
    public static final Color BORDE = new Color(0xE3, 0xE5, 0xEC);
    public static final Color VERDE = new Color(0x25, 0x9A, 0x4C);
    public static final Color NARANJA = new Color(0xDB, 0x8A, 0x14);
    public static final Color ROJO = new Color(0xD3, 0x3B, 0x3B);
    public static final Color SIDEBAR_SELECCION = new Color(0xFF, 0xF3, 0xC4);
    public static final Color FILA_ALTERNA = new Color(0xF6, 0xF7, 0xFC);

    public static final Font FUENTE_TITULO = new Font("Segoe UI Semibold", Font.PLAIN, 21);
    public static final Font FUENTE_SUBTITULO = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_NEGRITA = new Font("Segoe UI Semibold", Font.PLAIN, 13);

    private Theme() {
    }

    /** Aplica FlatLaf (look and feel plano y moderno) con acentos de marca en vez del Windows L&F por defecto. */
    public static void aplicarLookAndFeel() {
        try {
            FlatLightLaf.setup();

            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("CheckBox.arc", 4);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.showButtons", false);

            UIManager.put("defaultFont", FUENTE_NORMAL);
            UIManager.put("TitlePane.unifiedBackground", true);

            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.underlineColor", AZUL_MASS);
            UIManager.put("TabbedPane.hoverColor", new Color(0xEC, 0xEF, 0xFA));
            UIManager.put("TabbedPane.selectedBackground", BLANCO);

            UIManager.put("Table.selectionBackground", SIDEBAR_SELECCION);
            UIManager.put("Table.selectionForeground", AZUL_MASS);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.gridColor", BORDE);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
            UIManager.put("Table.rowHeight", 30);

            UIManager.put("MenuBar.background", BLANCO);
            UIManager.put("MenuBar.borderColor", BORDE);

            UIManager.put("ScrollBar.thumb", new Color(0xC9, 0xCD, 0xDA));
            UIManager.put("ScrollBar.track", FONDO);
        } catch (Exception ignored) {
        }
    }

    public static JButton botonPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(AMARILLO);
        b.setForeground(AZUL_MASS);
        b.setFont(FUENTE_NEGRITA);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.getModel().addChangeListener(e -> {
            if (b.getModel().isRollover()) {
                b.setBackground(AMARILLO_OSCURO);
            } else {
                b.setBackground(AMARILLO);
            }
        });
        return b;
    }

    public static JButton botonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(BLANCO);
        b.setForeground(TEXTO);
        b.setFont(FUENTE_NORMAL);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    public static JButton botonPeligro(String texto) {
        JButton b = botonSecundario(texto);
        b.setForeground(ROJO);
        return b;
    }

    /** Aplica un estilo consistente y elegante (encabezado en azul marino, filas alternadas) a cualquier tabla del sistema. */
    public static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(30);
        tabla.setFont(FUENTE_NORMAL);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(SIDEBAR_SELECCION);
        tabla.setSelectionForeground(AZUL_MASS);
        tabla.setFillsViewportHeight(true);
        tabla.setBackground(BLANCO);
        tabla.setDefaultRenderer(Object.class, new FilaAlternaRenderer());

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new EncabezadoTablaRenderer());
    }

    public static String estadoStock(int stock, int stockMinimo) {
        if (stock <= 0) return "Agotado";
        if (stock < stockMinimo) return "Stock bajo";
        return "Disponible";
    }

    public static Color colorEstadoStock(int stock, int stockMinimo) {
        if (stock <= 0) return ROJO;
        if (stock < stockMinimo) return NARANJA;
        return VERDE;
    }
}
