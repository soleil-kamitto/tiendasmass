# Plan de Monitoreo — Tiendas Mass (Taller de Monitoreo)

## 1. Alcance

Tiendas Mass es una aplicación de escritorio de un solo usuario por instancia (una
caja), no un servicio 24/7 con múltiples usuarios concurrentes ni un equipo de
guardia (on-call). El plan de monitoreo está dimensionado a eso: logs estructurados
locales + un chequeo de salud periódico, revisables por quien administra la tienda o
por soporte técnico cuando hay un problema — no un stack de observabilidad de
producción a gran escala (eso se señala como recomendación a futuro en la sección 6).

## 2. Logs

Framework: **SLF4J + Logback** (`pom.xml`, configuración en
`src/main/resources/logback.xml`). Se generan dos archivos, separados a propósito
para no mezclar eventos de negocio con métricas técnicas:

| Archivo | Contenido | Rotación |
|---|---|---|
| `logs/tiendas-mass.log` | Eventos de aplicación: arranque, esquema de BD, login, ventas, reposición de stock, errores no capturados | Diaria, retiene 30 días, tope 100MB |
| `logs/health.log` | Snapshots de salud del sistema (ver sección 4) | Diaria, retiene 14 días, tope 50MB |

Todos los archivos en UTF-8 explícito (evita mojibake con tildes/ñ
independientemente del locale del sistema operativo donde corra la app).

### Qué se loguea y en qué nivel

| Evento | Nivel | Clase |
|---|---|---|
| Login exitoso (usuario + rol) | INFO | `AuthService` |
| Login fallido (usuario inexistente / desactivado / contraseña incorrecta) | WARN | `AuthService` |
| Venta registrada (id, usuario, ítems, total, duración) | INFO | `VentaService` |
| Venta revertida por stock insuficiente u otro error (rollback) | WARN | `VentaService` |
| Error de conexión al registrar venta | ERROR | `VentaService` |
| Reposición de stock (producto, cantidad, usuario, motivo) | INFO | `InventarioService` |
| Error al reponer stock / de conexión | ERROR | `InventarioService` |
| Inicialización/errores de esquema y conexión a BD | INFO/ERROR | `Database` |
| Excepción no capturada en cualquier hilo (incluido el hilo de UI de Swing) | ERROR | `App` (handler global, ver `Thread.setDefaultUncaughtExceptionHandler`) |
| Chequeo de salud (BD + memoria JVM) | INFO / WARN si supera umbral | `HealthCheck` |

Deliberadamente **no** se loguean contraseñas ni hashes completos — solo el nombre
de usuario y el resultado (ver `SECURITY_TESTING.md` sobre manejo de credenciales).

## 3. Herramientas de rendimiento

- **Instrumentación propia:** `VentaService.registrarVenta` mide su propia duración
  (`System.nanoTime()`) y la deja en el log (`... (19 ms)`), porque es la transacción
  más sensible de la app (venta + descuento de stock + movimiento de inventario, todo
  o nada). Sirve para detectar degradación (ej. si empieza a tardar segundos en vez
  de milisegundos, algo anda mal con la BD).
- **Herramientas del JDK (sin dependencias nuevas), para inspección puntual:**
  - `jconsole` / VisualVM: conectar al proceso `java` de la app para ver heap,
    hilos y GC en vivo mientras se usa la aplicación.
  - `jcmd <pid> GC.heap_info` / `jcmd <pid> Thread.print`: snapshot puntual de
    memoria o hilos sin herramienta gráfica, útil para diagnosticar un cuelgue o
    consumo alto de memoria reportado por un usuario.

## 4. Herramientas de salud del sistema

`util.HealthCheck` corre automáticamente al iniciar la app y luego **cada 5
minutos** (`App.main` → `HealthCheck.iniciarChequeoPeriodico(5)`, hilo daemon, no
bloquea la UI) y loguea a `logs/health.log`:

- **Conectividad a la base de datos:** abre una conexión real (SQLite o el
  servidor MySQL configurado, ver `DEPLOYMENT.md`) y ejecuta `SELECT 1`, midiendo
  latencia. Si falla, queda en `ERROR` con el motivo.
- **Memoria de la JVM:** heap usado/máximo en MB y su porcentaje, más hilos
  activos. Si el uso supera el **85%** del máximo configurado, el chequeo sube de
  `INFO` a `WARN` — la señal de que conviene revisar antes de que la app empiece a
  fallar por falta de memoria.

Verificado en esta sesión: arrancando la app se ve en `logs/health.log` una línea
`BD: OK (N ms)` y otra `Memoria JVM: X/Y MB (Z%), hilos activos: N` apenas arranca,
y luego cada 5 minutos mientras la app siga abierta.

## 5. Umbrales y qué hacer si se disparan

| Señal en el log | Qué indica | Acción sugerida |
|---|---|---|
| Varios `WARN` de login fallido seguidos para el mismo usuario | Posible intento de fuerza bruta (ver hallazgo de "sin bloqueo de cuenta" en `SECURITY_TESTING.md`) | Revisar `tiendas-mass.log`, considerar implementar el bloqueo pendiente |
| `WARN` de venta revertida repetido para el mismo producto | Puede ser una alerta de stock mal calibrado (stock_minimo muy bajo) más que un bug | Revisar `InventarioPanel` / reponer stock |
| `ERROR` de conexión a BD (app o health check) | BD local corrupta/bloqueada, o servidor MySQL caído | Revisar `docker compose ps` si se usa el servidor; revisar permisos/espacio en disco si es SQLite |
| `WARN` de memoria JVM sobre 85% sostenido | Posible fuga de memoria o carga mayor a la esperada | Reiniciar la app; si persiste, investigar con VisualVM |

## 6. Revisión, retención y límites del alcance actual

- Al ser una app de una sola caja, la revisión de logs es manual (abrir
  `logs/tiendas-mass.log` / `logs/health.log` cuando algo se reporta como lento o
  raro), no hay un dashboard ni alerta automática push todavía.
- Retención: 30 días (app) / 14 días (salud), suficiente para depurar un problema
  reportado en la semana, sin crecer indefinidamente (`totalSizeCap` en
  `logback.xml`).
- **Para un despliegue a mayor escala** (varias cajas, servidor central MySQL de
  verdad en vez de Docker local, ver `DEPLOYMENT.md`) lo que le seguiría a este plan
  es centralizar los logs (ej. enviarlos a un agregador) y agregar alertas activas
  (ej. Prometheus + Grafana o un servicio administrado) en vez de depender de que
  alguien abra el archivo — se deja como recomendación explícita, no implementado,
  porque excede el alcance de una sola tienda con SQLite/MySQL local.

## 7. Cómo reproducir/verificar

```bat
mvnw.cmd clean package
java -jar target\tiendas-mass.jar
REM revisar logs\tiendas-mass.log y logs\health.log junto al jar
```

Al iniciar sesión, registrar una venta o reponer stock desde la UI, cada acción
queda reflejada en `logs/tiendas-mass.log` en tiempo real.
