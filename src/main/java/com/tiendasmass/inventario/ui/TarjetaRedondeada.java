package com.tiendasmass.inventario.ui;

import javax.swing.*;
import java.awt.*;

/** Panel con esquinas redondeadas y sombra suave, usado para "elevar" tarjetas sobre el fondo. */
public class TarjetaRedondeada extends JPanel {

    private final int arco;
    private final boolean sombra;

    public TarjetaRedondeada(int arco, boolean sombra) {
        this.arco = arco;
        this.sombra = sombra;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int margen = sombra ? 6 : 0;

        if (sombra) {
            for (int i = 4; i >= 0; i--) {
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(margen - i, margen - i + 2, w - 2 * margen + 2 * i, h - 2 * margen + 2 * i, arco, arco);
            }
        }

        g2.setColor(getBackground());
        g2.fillRoundRect(margen, margen, w - 2 * margen, h - 2 * margen, arco, arco);
        g2.dispose();
        super.paintComponent(g);
    }
}
