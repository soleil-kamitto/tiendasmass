package com.tiendasmass.inventario;

import com.tiendasmass.inventario.dao.ProductoDAO;
import com.tiendasmass.inventario.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reporte diario de stock (RF06/RF07): pensado para correr solo cada mañana
 * (ver scripts/reporte-stock-diario.bat) y dejar un archivo en reportes/ con el
 * stock actual y los productos por debajo del mínimo, además de imprimirlo por
 * consola. Reutiliza ProductoDAO — es el mismo dato que se ve en Productos e
 * Inventario dentro de la app, solo que generado sin abrir la interfaz.
 */
public class ReporteStock {

    private static final Logger log = LoggerFactory.getLogger(ReporteStock.class);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) throws IOException {
        ProductoDAO productoDAO = new ProductoDAO();
        List<Producto> todos = productoDAO.listarTodos();
        List<Producto> stockBajo = productoDAO.listarStockBajo();

        String contenido = generarReporte(todos, stockBajo);

        Path carpeta = Path.of("reportes");
        Files.createDirectories(carpeta);
        Path archivo = carpeta.resolve("stock_" + LocalDate.now() + ".txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(archivo))) {
            out.print(contenido);
        }

        System.out.print(contenido);
        log.info("Reporte de stock generado: {} ({} productos, {} bajo el mínimo)",
                archivo, todos.size(), stockBajo.size());
        for (Producto p : stockBajo) {
            log.warn("Stock bajo: {} ({}) - stock={}, mínimo={}",
                    p.getNombre(), p.getCodigo(), p.getStock(), p.getStockMinimo());
        }
    }

    private static String generarReporte(List<Producto> todos, List<Producto> stockBajo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reporte de stock — Tiendas Mass\n");
        sb.append("Generado: ").append(LocalDateTime.now().format(FORMATO_HORA)).append("\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append("ALERTAS DE STOCK BAJO (").append(stockBajo.size()).append(")\n");
        if (stockBajo.isEmpty()) {
            sb.append("  (ninguna)\n");
        } else {
            for (Producto p : stockBajo) {
                sb.append(String.format("  - %-30s [%s] stock=%-4d mínimo=%-4d%n",
                        p.getNombre(), p.getCodigo(), p.getStock(), p.getStockMinimo()));
            }
        }

        sb.append("\n").append("-".repeat(60)).append("\n");
        sb.append("STOCK ACTUAL (").append(todos.size()).append(" productos)\n");
        for (Producto p : todos) {
            String marca = p.isStockBajo() ? " *BAJO*" : "";
            sb.append(String.format("  %-30s [%s] stock=%-4d mínimo=%-4d%s%n",
                    p.getNombre(), p.getCodigo(), p.getStock(), p.getStockMinimo(), marca));
        }

        return sb.toString();
    }
}
