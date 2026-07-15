package com.tiendasmass.inventario.ui;

import javax.swing.*;
import java.awt.*;

public class EstadoStockRenderer extends FilaAlternaRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String estado = String.valueOf(value);
        Color color = switch (estado) {
            case "Disponible" -> Theme.VERDE;
            case "Stock bajo" -> Theme.NARANJA;
            default -> Theme.ROJO;
        };
        label.setText("● " + estado);
        label.setForeground(isSelected ? Theme.AZUL_MASS : color);
        label.setFont(Theme.FUENTE_NEGRITA);
        return label;
    }
}
