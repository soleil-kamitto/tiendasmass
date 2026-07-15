# Sistema de Inventario y Ventas — Tiendas Mass (SJL)

Implementación de la **Alternativa 1** del proyecto: aplicación de escritorio en Java
(Swing) con persistencia vía JDBC, pensada para instalarse localmente en cada caja y
seguir operando aunque la red de la tienda falle.

## Decisión técnica: SQLite en lugar de MySQL/Oracle

El documento del proyecto plantea MySQL/Oracle como motor de base de datos. Para esta
implementación se usó **SQLite** (driver `sqlite-jdbc`, incluido en `lib/`) porque:

- Se accede igual que cualquier otra base de datos **vía JDBC** (cumple RNF12 y la
  arquitectura de la Alternativa 1): cambiar a MySQL más adelante solo implica cambiar
  la cadena de conexión en `Database.java` y las sentencias SQL que usan sintaxis
  específica de SQLite (ninguna en este proyecto).
- No requiere instalar ni configurar un servidor de base de datos aparte: el archivo
  `tiendas_mass.db` se crea automáticamente junto al `.jar` la primera vez que se
  ejecuta el programa. Esto es ideal para que el profesor o cualquier compañero de
  equipo pueda correr el sistema sin pasos previos.
- Al ser un archivo local, refuerza el argumento de la Alternativa 1: el sistema sigue
  funcionando sin conexión a internet ni a un servidor central.

## Requisitos para ejecutar

- JDK 17 o superior (se probó con Eclipse Temurin 17).

## Look and feel

La interfaz usa [FlatLaf](https://www.formdev.com/flatlaf/) (`lib/flatlaf-3.5.4.jar`) en vez del
Look and Feel nativo de Windows, para lograr un acabado plano y moderno (bordes redondeados,
tablas con encabezado en azul marino y filas alternadas, sidebar oscuro con el logo de la marca).
Se configura en `Theme.aplicarLookAndFeel()`.

## Cómo compilar y ejecutar

Con **Maven** (recomendado; no requiere tener Maven instalado, el repo incluye el
Maven Wrapper):

```bat
mvnw.cmd clean package   REM compila, corre los tests y genera target\tiendas-mass.jar
java -jar target\tiendas-mass.jar
```

Alternativa sin Maven (scripts originales del proyecto, se conservan):

```bat
build.bat   REM compila el código y genera tiendas-mass.jar
run.bat     REM ejecuta el sistema
```

Ambos scripts detectan automáticamente un JDK instalado en el PATH o en la ruta
estándar de Eclipse Temurin 17 en Windows.

## Despliegue contra un servidor (Taller de Despliegue)

Ver [`DEPLOYMENT.md`](DEPLOYMENT.md): build con Maven, configuración de un servidor
MySQL real (`docker-compose.yml` + `run-server.bat`) como alternativa a SQLite local
sin cambiar código (solo variables de entorno `DB_URL`/`DB_USER`/`DB_PASSWORD`), y
las observaciones levantadas durante el despliegue (y cómo se resolvieron).

## Pruebas automatizadas (Taller de Testing)

```bat
test.bat    REM compila src/main y src/test, y corre toda la suite con JUnit 5
```

- Framework: **JUnit 5**, vía el jar autocontenido
  `lib/junit-platform-console-standalone-1.10.3.jar` (no requiere Maven/Gradle ni
  conexión a internet una vez descargado, en línea con el resto del proyecto).
- Cada test corre contra una **base SQLite temporal** (creada y destruida por
  `com.tiendasmass.inventario.support.TestDb`, con el mismo esquema y datos semilla
  que usa la app real vía `Database.initSchema()`), nunca contra `tiendas_mass.db`.
  Esto lo habilita `Database.setDbUrl(...)` / `resetDbUrl()`, agregado justamente
  para poder aislar las pruebas.
- Cobertura actual, sobre las reglas de negocio con más riesgo:
  - `PasswordUtilTest`: hashing con salt (RNF03) — salts distintos, verificación
    correcta/incorrecta, formato inválido.
  - `AuthServiceTest`: login válido, contraseña incorrecta, usuario inexistente y
    usuario desactivado (RF11/RF12).
  - `VentaServiceTest`: una venta descuenta el stock correctamente, y una venta con
    stock insuficiente no deja cambios a medias (RF04/RF05, transacción atómica).
  - `InventarioServiceTest`: reposición de stock y su historial (RF13/RF14),
    validación de cantidades inválidas.
- El reporte de la corrida (XML formato JUnit) queda en `test-reports/` después de
  ejecutar `test.bat`.

## Pruebas de seguridad (Taller de Pruebas de Seguridad)

Ver [`SECURITY_TESTING.md`](SECURITY_TESTING.md) para el reporte completo: metodología
(SpotBugs + FindSecBugs y revisión manual guiada por OWASP Top 10), los 10 hallazgos
levantados (con severidad y estado) y las pruebas de seguridad automatizadas en
`src/test/java/com/tiendasmass/inventario/security/`, que corren junto al resto de la
suite con `test.bat`. La evidencia cruda del análisis estático queda en
`security-reports/`.

## Usuarios de prueba (creados automáticamente en el primer arranque)

| Usuario  | Contraseña   | Rol            |
|----------|--------------|----------------|
| admin    | admin123     | ADMINISTRADOR  |
| empleado | empleado123  | EMPLEADO       |

Las contraseñas se guardan con hash SHA-256 + salt (`PasswordUtil`), nunca en texto
plano (RNF03).

## Estructura del proyecto

```
src/main/java/com/tiendasmass/inventario/
  App.java              punto de entrada
  db/Database.java      conexión JDBC + creación de esquema + datos semilla
  model/                entidades: Producto, Usuario, Cliente, Reclamo, Venta,
                         DetalleVenta, MovimientoInventario, Rol, TipoMovimiento
  dao/                  acceso a datos (una clase por tabla)
  service/               AuthService, VentaService, InventarioService — reglas de
                         negocio y transacciones (ej. venta + descuento de stock
                         + movimiento de inventario, todo o nada)
  ui/                    Swing: LoginFrame, MainFrame (menú + sidebar + paneles),
                         un panel por módulo
```

## Mapeo de requerimientos funcionales implementados

| Requerimiento | Dónde |
|---|---|
| RF01-03 Registrar/actualizar/eliminar productos | `ProductosPanel`, `ProductoDialog`, `ProductoDAO` |
| RF04-05 Registrar venta y descontar stock automáticamente | `VentasPanel`, `VentaService` (transacción JDBC) |
| RF06 Alertas de stock bajo | `Theme.estadoStock`, columna "Estado" en Productos/Inventario |
| RF07 Consulta de inventario en tiempo real | pestaña "Stock actual" en `InventarioPanel` |
| RF08 Reportes de ventas por periodo | pestaña "Ventas por periodo" en `ReportesPanel` |
| RF09 Rotación de productos (mayor/menor) | pestaña "Rotación de productos" en `ReportesPanel` |
| RF10, RF12 Gestión de usuarios y roles | `UsuariosPanel` (solo administrador) |
| RF11 Inicio de sesión | `LoginFrame`, `AuthService` |
| RF13-14 Historial de movimientos y reposición | `InventarioPanel`, `InventarioService` |
| RF15 Búsqueda de productos por nombre/código | campo de búsqueda en Productos y Ventas |

También se incluyen los módulos **Clientes** y **Reclamos** que aparecen en tus
diagramas ER/UML, aunque no tenían un RF numerado explícito, para que el sistema
sea consistente con el diseño ya sustentado.

## Nota sobre alcance

Este entregable cubre el sistema en sí (los RF/RNF y la Alternativa 1 seleccionada).
El Project Charter, WBS y diagrama de Gantt mencionados en los objetivos específicos
del documento son artefactos de gestión de proyecto separados del código y deben
incluirse en el documento, no en el software.
