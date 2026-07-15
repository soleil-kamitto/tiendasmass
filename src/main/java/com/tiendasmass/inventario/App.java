package com.tiendasmass.inventario;

import com.tiendasmass.inventario.db.Database;
import com.tiendasmass.inventario.ui.LoginFrame;
import com.tiendasmass.inventario.ui.Theme;
import com.tiendasmass.inventario.util.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Cualquier excepción no capturada (incluida la del Event Dispatch Thread de Swing)
        // queda en el log en vez de perderse en la consola (ver MONITORING.md).
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
                log.error("Excepción no capturada en el hilo '{}'", thread.getName(), ex));

        log.info("Iniciando Tiendas Mass...");
        Theme.aplicarLookAndFeel();

        Database.initSchema();
        HealthCheck.iniciarChequeoPeriodico(5);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("Tiendas Mass finalizado")));

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
