package com.tiendasmass.inventario.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Encabezado de tabla en azul marino con texto blanco, para reforzar la identidad de marca. */
public class EncabezadoTablaRenderer extends DefaultTableCellRenderer {

    public EncabezadoTablaRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
        setOpaque(true);
        setBorder(new EmptyBorder(0, 12, 0, 12));
        setFont(Theme.FUENTE_NEGRITA);
        setBackground(Theme.AZUL_MASS);
        setForeground(Theme.BLANCO);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        setText(value == null ? "" : value.toString());
        return this;
    }
}
