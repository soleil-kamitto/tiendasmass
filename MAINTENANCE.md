# Plan de Mantenimiento — Tiendas Mass (Taller de Mantenimiento)

## 1. Alcance

Mantenimiento operativo de la aplicación ya desplegada (ver `DEPLOYMENT.md` y
`MONITORING.md`): backups automáticos, restauración ante un problema, monitoreo
externo y administración del servidor, y los scripts que los soportan. Cubre
ambos motores de datos que soporta la app: SQLite local (default) y el servidor
MySQL de `docker-compose.yml`.

## 2. Categorías de scripts

Mapeando contra las 4 categorías de scripts de mantenimiento vistas en clase:

| Categoría | Script | Qué hace |
|---|---|---|
| Scripts de Backup | `scripts/backup.bat` | Respalda SQLite o MySQL según corresponda |
| Scripts de Automatización de Procesos | `scripts/instalar-tarea-backup.bat` | Registra la tarea programada que corre el backup solo, sin intervención humana |
| Scripts de Monitoreo | `scripts/monitoreo.bat` | Verifica el estado del servidor MySQL, espacio en disco y errores recientes en el log de la app |
| Scripts de Administración | `scripts/administracion.bat` | Gestiona el recurso del sistema operativo (el contenedor MySQL: iniciar/detener/estado) y su configuración (variables de entorno `DB_URL`/`DB_USER`/`DB_PASSWORD`) |
| *(extra, de negocio)* | `scripts/reporte-stock-diario.bat` | Reporte diario de stock para la tienda (no es infraestructura, es un "script relevante" orientado al negocio) |

## 3. Backups

**Script:** `scripts/backup.bat`. Detecta automáticamente qué motor está activo
(mira la variable de entorno `DB_URL`, la misma que usa la app — ver
`DEPLOYMENT.md`) y respalda lo que corresponda:

- **SQLite** (default): copia `tiendas_mass.db` a `backups/tiendas_mass_<fecha>.db`.
- **MySQL** (si `DB_URL` empieza con `jdbc:mysql:`): corre `mysqldump` dentro del
  contenedor `tiendasmass-mysql` y guarda el `.sql` resultante en
  `backups/mysql_tiendas_mass_<fecha>.sql`.

Cada corrida queda registrada en `backups/backup.log` (OK o FALLO, con fecha/hora).
Los backups de más de **30 días** se borran automáticamente en cada corrida
(`forfiles`), para no crecer indefinidamente.

**Verificado en esta sesión:** corrí `backup.bat` contra SQLite y contra el MySQL
real de `docker-compose.yml`; ambos generaron un archivo íntegro (el dump de MySQL
se confirmó con `INSERT INTO` reales para las 6 tablas).

### Tarea programada (cron)

Windows no tiene cron; su equivalente es el **Programador de tareas**
(Task Scheduler), manejado por línea de comandos con `schtasks`. Un cron job se
define con 5 campos (`minuto hora día-mes mes día-semana comando`); nuestro
backup diario a las 23:00 equivale a la expresión cron `0 23 * * *`, solo que
implementada con las herramientas de Windows en vez de un crontab de Linux.

- `scripts/instalar-tarea-backup.bat` — crea la tarea `TiendasMassBackupDiario`
  que corre `backup.bat` **todos los días a las 23:00** (requiere permisos de
  administrador la primera vez).
- `scripts/desinstalar-tarea-backup.bat` — la quita.
- Verificar que está activa: `schtasks /query /tn "TiendasMassBackupDiario"`.

**Estado actual:** la tarea ya está instalada en esta máquina (confirmado con
`schtasks /query`: próxima ejecución hoy 23:00, estado "Listo").

## 4. Restauración

**Script:** `scripts/restore.bat [archivo]`.

- Sin argumento, usa el backup más reciente en `backups/`.
- Detecta si el archivo es `.sql` (MySQL) o `.db` (SQLite) por su extensión y
  restaura al motor correspondiente.
- **Antes de sobrescribir nada**, guarda una copia de seguridad de lo que hay en
  ese momento (`backups/pre-restore_<fecha>.{db,sql}`) — la restauración también
  es reversible.
- Pide confirmación explícita (`s/n`) antes de tocar datos, porque es una
  operación destructiva sobre los datos actuales.

**Verificado en esta sesión** (extremo a extremo, no solo que corriera sin
error): en SQLite, restauré un backup y comparé el hash del archivo restaurado
contra el original (idénticos). En MySQL, corrompí a propósito el stock de un
producto (lo puse en 777), restauré desde un backup tomado antes con el valor
real (42), y confirmé en la base que volvió exactamente a 42.

## 5. Monitoreo externo

**Script:** `scripts/monitoreo.bat`. A diferencia de `util.HealthCheck` (que
corre *dentro* de la app cada 5 minutos, ver `MONITORING.md`), este es un script
**independiente**: no requiere que la app esté corriendo. Revisa:

- Si el contenedor `tiendasmass-mysql` está activo (si se usa el servidor).
- Espacio libre en la unidad de disco donde vive el proyecto (advierte si
  quedan menos de 2 GB — relevante porque ahí se guardan los backups).
- Si `logs/tiendas-mass.log` tiene alguna línea `ERROR` reciente.

Deja su propio registro en `logs/monitoreo-externo.log` y también imprime un
resumen en pantalla. Se puede correr solo o agregarlo a una tarea programada
igual que el backup.

## 6. Administración

**Script:** `scripts/administracion.bat [estado|iniciar-servidor|detener-servidor|configurar-entorno]`.
Gestiona el recurso del sistema operativo que necesita esta app (el contenedor
MySQL) y su configuración (las variables de entorno de conexión):

- `estado` — si el contenedor MySQL está corriendo, versión de Java instalada,
  y qué `DB_URL`/`DB_USER` están activos en la sesión actual.
- `iniciar-servidor` / `detener-servidor` — `docker compose up -d` / `down`
  sobre `docker-compose.yml`, para no tener que recordar el comando.
- `configurar-entorno` — guarda `DB_URL`/`DB_USER`/`DB_PASSWORD` como variables
  de entorno **permanentes** del usuario de Windows (`setx`), para no tener que
  definirlas a mano cada vez que se quiere correr contra el servidor.

**Verificado en esta sesión:** los 4 subcomandos, incluyendo detener y volver a
levantar el contenedor real, y guardar/limpiar las variables de entorno
persistentes (se revirtieron después de probar, para no dejar la máquina de
desarrollo apuntando a MySQL por defecto sin que nadie lo haya pedido).

## 7. Reporte diario de stock

**Script:** `scripts/reporte-stock-diario.bat` → corre
`com.tiendasmass.inventario.ReporteStock` (clase Java nueva, reutiliza
`ProductoDAO` — el mismo dato que se ve en Productos/Inventario dentro de la
app). Este no es un script de infraestructura como los anteriores, sino un
**reporte de negocio**: cubre "scripts relevantes" de la rúbrica desde el lado
de la tienda, no solo del sistema.

Genera `reportes/stock_<fecha>.txt` con:
- Las alertas de stock bajo (RF06: productos con `stock < stock_minimo`), primero.
- El listado completo de stock actual, marcando los que están bajo el mínimo.

También loguea (vía SLF4J, igual que el resto de la app) un `WARN` por cada
producto con stock bajo, para que quede en `logs/tiendas-mass.log` igual que
cualquier otra alerta del sistema.

**Tarea programada:** `scripts/instalar-tarea-reporte.bat` registra
`TiendasMassReporteStockDiario`, que corre el reporte **todos los días a las
7:00 am** — para que quien abre la tienda tenga el estado del stock antes de
abrir. `scripts/desinstalar-tarea-reporte.bat` la quita.

**Verificado en esta sesión:** corrido contra los datos reales de la app;
detectó correctamente los 2 productos con stock bajo (Gaseosa Kola Real y Leche
Gloria) de los 6 productos sembrados, y generó el archivo con acentos/ñ
correctos (UTF-8). Tarea programada instalada y confirmada con `schtasks /query`
(próxima ejecución: mañana 07:00).

## 8. Observaciones levantadas durante este taller (y cómo se resolvieron)

- **`restore.bat`:** con bloques `if/else` anidados (el mismo estilo usado en
  el resto del proyecto), `cmd.exe` fallaba con *"El sistema no encuentra la
  etiqueta por lotes especificada"* al saltar sobre el bloque de MySQL para
  llegar al de SQLite — un problema de parseo de `cmd.exe` relacionado con un
  `goto` que debía saltar por encima de una línea con `docker exec -i ... | ...`.
  Además, la detección de si un archivo era `.sql` vía `findstr /e` fallaba
  siempre (un espacio final invisible que agrega `echo` al canalizar su salida
  rompía el anclaje de fin de línea). Se resolvieron reescribiendo el flujo con
  `goto`/etiquetas, **ordenando la rama SQLite antes que la de MySQL** (evita el
  salto problemático), cambiando el pipe por redirección de entrada (`<`), y
  reemplazando `findstr` por una comparación de subcadena (`%ARCHIVO:~-4%`).
- **`administracion.bat`:** el mismo tipo de problema apareció otra vez —un
  `goto` que saltaba por encima de un bloque con
  `for /f ... in ('docker ps -q -f ... 2^>nul') do ...` rompía la resolución de
  la etiqueta destino. Como aquí había 4 acciones posibles (cualquiera podía ser
  la que se pide), no bastaba con reordenar. Se resolvió eliminando `goto`
  /etiquetas del todo para el despacho de comandos: cada acción es un bloque
  `if "%~1"=="..." ( ... exit /b 0 )` independiente que se ejecuta en línea, sin
  necesitar saltar sobre ningún otro bloque.
- **`tiendas_mass.db` corrompido durante las pruebas de `restore.bat`:** antes de
  arreglar la detección de extensión `.sql` (ver arriba), una prueba de
  restauración de un backup de MySQL tomó por error la rama de SQLite y copió el
  contenido del dump `.sql` directamente sobre `tiendas_mass.db` real. No se
  perdieron datos reales (todo lo que había ahí era de pruebas de esta misma
  sesión), pero sí quedó un archivo inválido sin que se notara hasta más tarde,
  al usar el reporte de stock. Se corrigió regenerando el esquema y los datos
  semilla (`Database.initSchema()`). **Lección aplicada:** al probar un script
  destructivo (restore, en este caso), verificar el archivo real afectado
  inmediatamente después de cada corrida, no solo el mensaje de "OK" que
  imprime el script.

## 9. Otras tareas de mantenimiento ya cubiertas en talleres anteriores

- **Logs:** rotación y límite de tamaño ya automatizados por Logback
  (`maxHistory`, `totalSizeCap` en `logback.xml`) — no requieren un script aparte,
  ver `MONITORING.md`.
- **Dependencias:** versiones fijadas explícitamente en `pom.xml` (no rangos
  abiertos), para que una actualización de una librería sea un cambio deliberado,
  no automático.

## 10. Qué falta para un mantenimiento a mayor escala (fuera de alcance aquí)

Igual que se señaló en `DEPLOYMENT.md`/`MONITORING.md`: para varias cajas o un
servidor MySQL de producción real (no Docker local), lo siguiente sería subir los
backups a almacenamiento externo (no solo al disco local de la misma máquina que
se quiere proteger) y agregar una alerta activa si `backup.log` no registra un
"OK" en 24 horas — no implementado aquí porque excede el alcance de una sola
tienda con backups locales.

## 11. Cómo reproducir

```bat
scripts\backup.bat                              REM backup manual (o esperar la tarea programada de las 23:00)
scripts\restore.bat                             REM restaura el backup mas reciente (pide confirmacion)
scripts\restore.bat backups\archivo_especifico  REM restaura uno puntual
scripts\instalar-tarea-backup.bat               REM registra el backup diario en el Programador de tareas
scripts\desinstalar-tarea-backup.bat            REM lo quita
scripts\monitoreo.bat                           REM chequeo de salud independiente de la app
scripts\administracion.bat estado               REM estado del servidor MySQL y de Java
scripts\administracion.bat iniciar-servidor     REM levanta el servidor MySQL
scripts\administracion.bat detener-servidor     REM lo detiene
scripts\administracion.bat configurar-entorno   REM guarda DB_URL/DB_USER/DB_PASSWORD de forma permanente
scripts\reporte-stock-diario.bat                REM genera reportes\stock_<fecha>.txt
scripts\instalar-tarea-reporte.bat              REM registra el reporte diario en el Programador de tareas (7:00 am)
scripts\desinstalar-tarea-reporte.bat           REM lo quita
```
