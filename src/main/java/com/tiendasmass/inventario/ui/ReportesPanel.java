package com.tiendasmass.inventario.ui;

import com.tiendasmass.inventario.dao.VentaDAO;
import com.tiendasmass.inventario.model.Venta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** RF08 (reportes de ventas por periodo) y RF09 (rotación de productos: mayor/menor). */
public class ReportesPanel extends JPanel implements Refrescable {

    private final VentaDAO ventaDAO = new VentaDAO();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JTextField campoDesde = new JTextField(LocalDate.now().minusDays(6).toString());
    private final JTextField campoHasta = new JTextField(LocalDate.now().toString());

    private final DefaultTableModel modeloVentas = new DefaultTableModel(
            new String[]{"N° Venta", "Fecha", "Cliente", "Atendido por", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel modeloRotacion = new DefaultTableModel(
            new String[]{"Producto", "Unidades vendidas", "Total facturado"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel etiquetaResumen = new JLabel();

    public ReportesPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.FONDO);

        add(construirBarraFiltros(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        JTable tablaVentas = new JTable(modeloVentas);
        estilo(tablaVentas);
        tabs.addTab("Ventas por periodo (RF08)", new JScrollPane(tablaVentas));

        JTable tablaRotacion = new JTable(modeloRotacion);
        estilo(tablaRotacion);
        JPanel rotacionPanel = new JPanel(new BorderLayout());
        rotacionPanel.add(new JScrollPane(tablaRotacion), BorderLayout.CENTER);
        JLabel nota = new JLabel("  Ordenado de mayor a menor rotación: el primero es el de mayor rotación, el último el de menor rotación.");
        nota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        nota.setForeground(Theme.TEXTO_SUAVE);
        rotacionPanel.add(nota, BorderLayout.SOUTH);
        tabs.addTab("Rotación de productos (RF09)", rotacionPanel);

        add(tabs, BorderLayout.CENTER);

        etiquetaResumen.setFont(Theme.FUENTE_NEGRITA);
        add(etiquetaResumen, BorderLayout.SOUTH);

        generarReporte();
    }

    private void estilo(JTable tabla) {
        Theme.estilizarTabla(tabla);
    }

    private JComponent construirBarraFiltros() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setBackground(Theme.FONDO);

        campoDesde.setPreferredSize(new Dimension(110, 28));
        campoHasta.setPreferredSize(new Dimension(110, 28));

        JButton hoy = Theme.botonSecundario("Hoy");
        JButton semana = Theme.botonSecundario("Esta semana");
        JButton mes = Theme.botonSecundario("Este mes");
        JButton generar = Theme.botonPrimario("Generar reporte");

        hoy.addActionListener(e -> establecerRango(LocalDate.now(), LocalDate.now()));
        semana.addActionListener(e -> establecerRango(LocalDate.now().minusDays(6), LocalDate.now()));
        mes.addActionListener(e -> establecerRango(LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        generar.addActionListener(e -> generarReporte());

        barra.add(new JLabel("Desde (AAAA-MM-DD):"));
        barra.add(campoDesde);
        barra.add(new JLabel("Hasta:"));
        barra.add(campoHasta);
        barra.add(hoy);
        barra.add(semana);
        barra.add(mes);
        barra.add(generar);
        return barra;
    }

    private void establecerRango(LocalDate desde, LocalDate hasta) {
        campoDesde.setText(desde.toString());
        campoHasta.setText(hasta.toString());
        generarReporte();
    }

    private void generarReporte() {
        LocalDateTime desde, hasta;
        try {
            desde = LocalDate.parse(campoDesde.getText().trim()).atStartOfDay();
            hasta = LocalDate.parse(campoHasta.getText().trim()).atTime(23, 59, 59);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Usa AAAA-MM-DD.", "Fecha inválida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        modeloVentas.setRowCount(0);
        List<Venta> ventas = ventaDAO.listarEntre(desde, hasta);
        BigDecimal totalPeriodo = BigDecimal.ZERO;
        for (Venta v : ventas) {
            modeloVentas.addRow(new Object[]{
                    v.getId(), v.getFecha().format(FORMATO_FECHA), v.getClienteNombre(),
                    v.getUsuarioNombre(), "S/ " + v.getTotal()
            });
            totalPeriodo = totalPeriodo.add(v.getTotal());
        }

        modeloRotacion.setRowCount(0);
        for (Object[] fila : ventaDAO.rotacionProductos(desde, hasta)) {
            modeloRotacion.addRow(new Object[]{fila[0], fila[1], "S/ " + fila[2]});
        }

        etiquetaResumen.setText(ventas.size() + " venta(s) en el periodo · Total facturado: S/ "
                + totalPeriodo.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Override
    public void refrescar() {
        generarReporte();
    }
}
