package com.tiendasmass.inventario;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.ui.LoginFrame;
import com.tiendasmass.inventario.ui.Theme;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        Theme.aplicarLookAndFeel();

        Database.initSchema();

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
