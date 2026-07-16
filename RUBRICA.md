# Cobertura de la Rúbrica — Tiendas Mass

Este documento reúne, en un solo lugar, cada rúbrica del curso, qué se hizo en el
proyecto para cubrirla y dónde está la evidencia. Es el punto de partida para
sustentar el proyecto; cada sección enlaza al documento detallado correspondiente.

## El proyecto en una línea

Sistema de inventario y ventas de escritorio para Tiendas Mass (SJL), en Java
Swing con persistencia JDBC — SQLite local por defecto (Alternativa 1 del
enunciado, para seguir funcionando sin red) y MySQL como servidor opcional. Ver
`README.md` para el detalle funcional (RF/RNF) y la estructura del código.

Repositorio: `github.com/soleil-kamitto/tiendasmass`.

## Resumen

| Taller | % | Estado | Documento |
|---|---|---|---|
| Testing | 60% | ✅ | (ver más abajo, sin doc aparte) |
| Pruebas de Seguridad | 70% | ✅ | [`SECURITY_TESTING.md`](SECURITY_TESTING.md) |
| Despliegue | 80% | ✅ | [`DEPLOYMENT.md`](DEPLOYMENT.md) |
| Monitoreo | 90% | ✅ | [`MONITORING.md`](MONITORING.md) |
| Mantenimiento | 100% | ✅ | [`MAINTENANCE.md`](MAINTENANCE.md) |

---

## Taller de Testing (60%)

> "Se califica que el estudiante demuestre integración de las pruebas de software
> y seguridad en el desarrollo de proyectos [...] En el Taller de Testing, se
> espera que el estudiante entregue avances al 60% de los proyectos, demostrando
> el uso efectivo de las pruebas de software y recibiendo retroalimentación."

**Qué se hizo:**
- Suite de **JUnit 5** en `src/test/java/com/tiendasmass/inventario/`:
  `PasswordUtilTest`, `AuthServiceTest`, `VentaServiceTest`, `InventarioServiceTest`
  — cubren la lógica de negocio con más riesgo (hashing de contraseñas, login,
  la transacción de venta con descuento de stock, reposición de inventario).
- Cada test corre contra una **base SQLite temporal** (`support/TestDb.java`),
  nunca contra `tiendas_mass.db` real — se agregó `Database.setDbUrl()` /
  `resetDbUrl()` específicamente para poder aislar los tests.
- Se puede correr con `mvnw.cmd test` (Maven) o con `test.bat` (sin Maven, jar
  autocontenido de JUnit en `lib/`) — ambos caminos funcionan.

## Taller de Pruebas de Seguridad (70%)

> "En el Taller de Pruebas de Seguridad, se espera una entrega de avances al 70%
> de los proyectos, junto con observaciones levantadas y un reporte detallado de
> las pruebas de seguridad realizadas."

**Qué se hizo** (detalle completo en [`SECURITY_TESTING.md`](SECURITY_TESTING.md)):
- Análisis estático con **SpotBugs + FindSecBugs** sobre el código propio
  (evidencia cruda en `security-reports/`).
- Revisión manual guiada por **OWASP Top 10**.
- **10 hallazgos documentados** con severidad: 2 corregidos (escalada de
  privilegios en memoria vía `Session` mutable — hallazgo real, verificado con
  un test que reproduce el ataque y confirma la corrección; y un uso ineficiente
  de `SecureRandom`), 3 verificados como no-vulnerables con pruebas dinámicas
  (inyección SQL, enumeración de usuarios, contraseñas en texto plano), 5 quedan
  como observaciones documentadas para trabajo futuro (con su severidad y
  recomendación).
- 6 tests de seguridad en `src/test/java/.../security/` (`SqlInjectionTest`,
  `PasswordStorageTest`, `SessionPrivilegeEscalationTest`).

## Taller de Despliegue (80%)

> "Se califica que el estudiante demuestre un conocimiento sólido en el
> despliegue de aplicaciones Java, haciendo uso de Maven y configurando
> servidores para garantizar el soporte adecuado de la aplicación desarrollada."

**Qué se hizo** (detalle completo en [`DEPLOYMENT.md`](DEPLOYMENT.md)):
- Migración del build de scripts `javac`/`jar` a mano a **Maven** (`pom.xml` +
  Maven Wrapper `mvnw`/`mvnw.cmd`, para que no haga falta tener Maven instalado),
  con `maven-shade-plugin` generando el jar ejecutable.
- **Servidor MySQL real** (no solo local): `docker-compose.yml` levanta un MySQL
  8.4, y `Database.java` se conecta a él vía variables de entorno
  (`DB_URL`/`DB_USER`/`DB_PASSWORD`) sin cambiar código — SQLite sigue siendo el
  default para no romper el diseño de la Alternativa 1.
- Verificado de punta a punta contra el servidor real: login, y una venta que
  descuenta stock correctamente, confirmado directamente en las tablas de MySQL.
- **Observación real levantada y corregida:** el jar generado por
  `maven-shade-plugin` perdía el registro SPI de uno de los dos drivers JDBC
  (SQLite y MySQL comparten `META-INF/services/java.sql.Driver`); se agregó el
  `ServicesResourceTransformer` y se confirmó que ambos drivers quedan
  registrados en el jar final.

## Taller de Monitoreo (90%)

> "Se califica que el estudiante demuestre un dominio de las mejores prácticas
> para el monitoreo de aplicaciones, abarcando el uso de logs, herramientas de
> rendimiento y herramientas de salud del sistema [...] elabore un plan de
> monitoreo exhaustivo."

**Qué se hizo** (detalle completo en [`MONITORING.md`](MONITORING.md)):
- **Logs estructurados** con SLF4J + Logback: `logs/tiendas-mass.log` (login,
  ventas, reposición de stock, errores) rotado y con retención de 30 días.
- **Salud del sistema:** `util.HealthCheck` corre al arrancar y cada 5 minutos,
  chequea conectividad real a la BD y memoria de la JVM, con alerta si supera el
  85% de uso — todo en `logs/health.log`.
- **Rendimiento:** la transacción de venta mide y loguea su propia duración; se
  documentó además cómo usar JConsole/VisualVM/jcmd (del propio JDK) para
  inspección puntual, sin agregar dependencias nuevas.
- Handler global de excepciones no capturadas (incluidas las del hilo de Swing).

## Taller de Mantenimiento (100%)

> "Se califica que el estudiante demuestre un dominio de las mejores prácticas
> para el mantenimiento de aplicaciones, lo cual incluye la implementación
> efectiva de cron jobs, backups y scripts relevantes [...] elabore un plan de
> mantenimiento integral."

**Qué se hizo** (detalle completo en [`MAINTENANCE.md`](MAINTENANCE.md)):
- **Backups** (`scripts/backup.bat`): detecta automáticamente SQLite o MySQL y
  respalda lo que corresponda, con retención de 30 días.
- **Restauración** (`scripts/restore.bat`): guarda una copia de seguridad de lo
  actual antes de sobrescribir (reversible), pide confirmación explícita.
- **Cron job** (Windows no tiene cron; su equivalente es el Programador de
  tareas): `scripts/instalar-tarea-backup.bat` registra una tarea diaria a las
  23:00 (equivalente a la expresión cron `0 23 * * *`) — **ya instalada** en la
  máquina de desarrollo, confirmada con `schtasks /query`.
- **Monitoreo externo** (`scripts/monitoreo.bat`) y **administración**
  (`scripts/administracion.bat`): scripts independientes de la app — el primero
  revisa servidor MySQL/disco/errores en el log, el segundo gestiona el
  contenedor MySQL (iniciar/detener/estado) y la configuración de conexión.
- **Reporte diario de stock** (`scripts/reporte-stock-diario.bat`, con su propia
  tarea programada a las 7:00 am): un "script relevante" del lado del negocio,
  no solo de infraestructura — reutiliza `ProductoDAO` para dejar un archivo
  con las alertas de stock bajo (RF06) cada mañana.
- Verificado extremo a extremo (no solo que corriera sin error): se corrompió a
  propósito un valor en SQLite y en MySQL, se restauró desde un backup anterior,
  y se confirmó que el valor volvió exactamente al original en ambos casos.
- **Observación real levantada y corregida:** al escribir `restore.bat`, un
  `goto` de `cmd.exe` rompía la resolución de una etiqueta al tener que saltar
  por encima de una línea con `docker exec -i ... | ...` (pipe); además,
  `findstr /e` nunca detectaba la extensión `.sql` por un espacio final
  invisible que agrega `echo` al canalizar su salida. Se corrigieron
  reordenando las ramas del script, cambiando el pipe por redirección de
  entrada (`<`), y reemplazando `findstr` por una comparación de subcadena.

---

## Rúbrica general: "solución informática"

> "El estudiante construye una solución informática considerando: 1) completa
> (cubre el alcance comprometido), 2) coherente (la documentación y el código
> están alineados), 3) buenas prácticas (usa librerías adecuadas, patrones de
> diseño, software de control de versiones, entre otros) y 4) autoría (el código
> fue hecho por el estudiante o lo domina)."

1. **Completa:** los RF/RNF del enunciado están implementados (ver tabla en
   `README.md`) y los 5 talleres de arriba están cubiertos con evidencia
   verificada, no solo documentada.
2. **Coherente:** cada documento (`DEPLOYMENT.md`, `MONITORING.md`,
   `SECURITY_TESTING.md`, `MAINTENANCE.md`) se actualizó junto con el código al
   que describe, en el mismo commit; el `README.md` enlaza a todos.
3. **Buenas prácticas:** Maven con versiones fijadas explícitamente, patrón
   DAO/Service para separar acceso a datos de lógica de negocio, hashing de
   contraseñas con salt, control de versiones con Git/GitHub, tests
   automatizados, logging estructurado.
4. **Autoría:** este es el único criterio que no se puede "completar" con más
   código. El proyecto se construyó con asistencia de Claude a partir de tus
   instrucciones; para defenderlo necesitas poder explicar las decisiones clave
   si te preguntan. Ver [`GUIA_SUSTENTACION.md`](GUIA_SUSTENTACION.md) — una
   guía en lenguaje simple, con las preguntas típicas de cada taller y las
   respuestas en tus palabras. Puntos concretos que conviene repasar antes de
   sustentar (desarrollados en la guía):
   - Por qué `Session.actual()` devuelve una copia del usuario y no la
     referencia real (`util/Session.java`) — el hallazgo de seguridad más
     importante del proyecto.
   - Cómo `Database.java` decide entre SQLite y MySQL sin cambiar código
     (variables de entorno `DB_URL`/`DB_USER`/`DB_PASSWORD`).
   - Por qué el jar de Maven necesitaba el `ServicesResourceTransformer` (dos
     drivers JDBC compitiendo por el mismo archivo de registro).
   - La estructura de `VentaService.registrarVenta`: por qué todo pasa en una
     sola transacción JDBC (`conn.setAutoCommit(false)` + `commit`/`rollback`).

Si quieres, en la próxima sesión repasamos estos puntos juntos.
