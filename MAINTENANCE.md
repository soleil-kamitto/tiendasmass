# Plan de Mantenimiento — Tiendas Mass (Taller de Mantenimiento)

## 1. Alcance

Mantenimiento operativo de la aplicación ya desplegada (ver `DEPLOYMENT.md` y
`MONITORING.md`): backups automáticos, restauración ante un problema, y los
scripts que los soportan. Cubre ambos motores de datos que soporta la app: SQLite
local (default) y el servidor MySQL de `docker-compose.yml`.

## 2. Backups

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
(Task Scheduler), manejado por línea de comandos con `schtasks`.

- `scripts/instalar-tarea-backup.bat` — crea la tarea `TiendasMassBackupDiario`
  que corre `backup.bat` **todos los días a las 23:00** (requiere permisos de
  administrador la primera vez).
- `scripts/desinstalar-tarea-backup.bat` — la quita.
- Verificar que está activa: `schtasks /query /tn "TiendasMassBackupDiario"`.

**Estado actual:** la tarea ya está instalada en esta máquina (confirmado con
`schtasks /query`: próxima ejecución hoy 23:00, estado "Listo").

## 3. Restauración

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

## 4. Observación levantada durante este taller (y cómo se resolvió)

Al escribir `restore.bat` con bloques `if/else` anidados (el mismo estilo usado
en el resto del proyecto), `cmd.exe` fallaba con
*"El sistema no encuentra la etiqueta por lotes especificada"* al saltar sobre el
bloque de MySQL para llegar al de SQLite — un problema de parseo de `cmd.exe`
relacionado con un `goto` que debía saltar por encima de una línea con
`docker exec -i ... | ...`. Además, la detección de si un archivo era `.sql` vía
`findstr /e` fallaba siempre (un espacio final invisible que agrega `echo` al
canalizar su salida rompía el anclaje de fin de línea). Se resolvieron:
reescribiendo el flujo con `goto`/etiquetas en vez de bloques anidados, **ordenando
la rama SQLite antes que la de MySQL** en el archivo (evita el salto problemático),
reemplazando el pipe hacia `docker exec -i` por redirección de entrada (`<`), y
cambiando la detección de extensión por una comparación de subcadena
(`%ARCHIVO:~-4%`) en vez de `findstr`. Todo esto se verificó de nuevo después del
cambio (ver sección 3).

## 5. Otras tareas de mantenimiento ya cubiertas en talleres anteriores

- **Logs:** rotación y límite de tamaño ya automatizados por Logback
  (`maxHistory`, `totalSizeCap` en `logback.xml`) — no requieren un script aparte,
  ver `MONITORING.md`.
- **Dependencias:** versiones fijadas explícitamente en `pom.xml` (no rangos
  abiertos), para que una actualización de una librería sea un cambio deliberado,
  no automático.

## 6. Qué falta para un mantenimiento a mayor escala (fuera de alcance aquí)

Igual que se señaló en `DEPLOYMENT.md`/`MONITORING.md`: para varias cajas o un
servidor MySQL de producción real (no Docker local), lo siguiente sería subir los
backups a almacenamiento externo (no solo al disco local de la misma máquina que
se quiere proteger) y agregar una alerta activa si `backup.log` no registra un
"OK" en 24 horas — no implementado aquí porque excede el alcance de una sola
tienda con backups locales.

## 7. Cómo reproducir

```bat
scripts\backup.bat                              REM backup manual (o esperar la tarea programada de las 23:00)
scripts\restore.bat                             REM restaura el backup mas reciente (pide confirmacion)
scripts\restore.bat backups\archivo_especifico  REM restaura uno puntual
scripts\instalar-tarea-backup.bat               REM registra el backup diario en el Programador de tareas
scripts\desinstalar-tarea-backup.bat            REM lo quita
```
