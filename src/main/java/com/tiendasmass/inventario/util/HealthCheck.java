package com.tiendasmass.inventario.util;

import com.tiendasmass.inventario.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Salud del sistema para el Taller de Monitoreo (ver MONITORING.md): conectividad a la
 * base de datos y uso de memoria de la JVM, logueados periódicamente en logs/health.log
 * (logger dedicado configurado en logback.xml).
 */
public final class HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);
    private static final long UMBRAL_MEMORIA_WARN_PORCENTAJE = 85;

    private HealthCheck() {
    }

    /** Corre una vez y loguea el resultado (conexión a BD + memoria de la JVM). */
    public static void chequearAhora() {
        chequearBaseDeDatos();
        chequearMemoria();
    }

    /** Programa el chequeo para correr cada {@code intervaloMinutos} en un hilo daemon. */
    public static ScheduledExecutorService iniciarChequeoPeriodico(long intervaloMinutos) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-check");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(HealthCheck::chequearAhora, 0, intervaloMinutos, TimeUnit.MINUTES);
        return executor;
    }

    private static void chequearBaseDeDatos() {
        long inicio = System.nanoTime();
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SELECT 1");
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            log.info("BD: OK ({} ms)", ms);
        } catch (Exception e) {
            log.error("BD: FALLO al conectar/consultar - {}", e.getMessage());
        }
    }

    private static void chequearMemoria() {
        Runtime rt = Runtime.getRuntime();
        long usadaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long porcentaje = maxMb == 0 ? 0 : (usadaMb * 100) / maxMb;

        String linea = String.format("Memoria JVM: %d/%d MB (%d%%), hilos activos: %d",
                usadaMb, maxMb, porcentaje, Thread.activeCount());

        if (porcentaje >= UMBRAL_MEMORIA_WARN_PORCENTAJE) {
            log.warn("{} - por encima del umbral de {}%", linea, UMBRAL_MEMORIA_WARN_PORCENTAJE);
        } else {
            log.info(linea);
        }
    }
}
