package com.tiendasmass.inventario.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Renderer base con relleno horizontal y franjas alternadas para que las tablas se vean menos planas. */
public class FilaAlternaRenderer extends DefaultTableCellRenderer {

    public FilaAlternaRenderer() {
        setBorder(new EmptyBorder(0, 12, 0, 12));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
            label.setBackground(row % 2 == 0 ? Theme.BLANCO : Theme.FILA_ALTERNA);
            label.setForeground(Theme.TEXTO);
        }
        label.setBorder(new EmptyBorder(0, 12, 0, 12));
        return label;
    }
}
