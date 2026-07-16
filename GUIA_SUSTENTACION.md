# Guía para sustentar el proyecto

Esto no es para memorizar de corrido. Es para que entiendas **qué hay y por qué**,
en tus palabras, y tengas lista una respuesta cuando te pregunten. Léelo una vez
completo, y repasa la sección del taller que más te pregunten antes de sustentar.

---

## 1. El proyecto en 30 segundos (memoriza esto textual si puedes)

"Es un sistema de inventario y ventas para una tienda, hecho en Java de escritorio
(no web). Cada caja tiene su propia instalación con su propia base de datos local
(SQLite), así que sigue funcionando aunque se caiga el internet de la tienda. El
enunciado original pedía MySQL/Oracle, pero como se accede todo vía JDBC (el
estándar de Java para hablar con bases de datos), cambiar de SQLite a un servidor
real es solo cambiar la dirección de conexión, no reescribir código — y de hecho
eso es exactamente lo que se demostró en el Taller de Despliegue."

## 2. Cómo está organizado el código (arquitectura en capas)

Si te preguntan "explica la arquitectura", la idea clave es: **cada carpeta tiene
una responsabilidad, y no se mezclan**.

- `model/` — las "cosas" del negocio: un Producto, un Usuario, una Venta. Solo
  datos, sin lógica.
- `dao/` — el único lugar que habla con la base de datos (SQL). Un DAO por tabla.
- `service/` — las reglas de negocio: "vender un producto implica descontar
  stock Y registrar el movimiento, todo junto o nada" — eso vive en
  `VentaService`, no en la pantalla.
- `ui/` — las ventanas (Swing). Solo se encargan de mostrar cosas y llamar a los
  `service`, no tienen lógica de negocio propia.

**Por qué importa:** si te preguntan "¿por qué no pusiste el SQL directo en el
botón de la pantalla?", la respuesta es: separar capas hace que se pueda probar
la lógica de negocio sin abrir la interfaz gráfica (por eso el Taller de Testing
pudo probar `VentaService` directamente), y que cambiar de SQLite a MySQL no
afecte a la UI para nada.

---

## 3. Taller de Testing (60%)

**¿Qué es "testing" acá?** Código que prueba automáticamente que el resto del
código funciona bien, sin que un humano tenga que abrir la app y hacer clic en
todo cada vez que se cambia algo.

**¿Qué se probó y por qué esas cosas?** Las partes con más riesgo de romperse
silenciosamente:
- `PasswordUtil` — que el hash de una contraseña sea distinto cada vez (por el
  salt aleatorio) y que reconozca la contraseña correcta/incorrecta.
- `AuthService` — login correcto, contraseña mala, usuario que no existe, usuario
  desactivado.
- `VentaService` — que vender descuente el stock bien, y que si no hay stock
  suficiente, **no quede nada a medias** (ni se registra la venta ni se descuenta
  stock — o pasa todo o no pasa nada).
- `InventarioService` — reponer stock, y que rechace cantidades inválidas (0 o
  negativas).

**Pregunta típica: "¿los tests tocan la base de datos real?"**
No — y esto es importante que lo sepas explicar. Se creó una base SQLite
**temporal** para cada test (un archivo descartable), usando
`Database.setDbUrl(...)`. Así los tests nunca corrompen `tiendas_mass.db`, que es
la base real de la app.

**Cómo correrlo si te lo piden en vivo:** `mvnw.cmd test` (o `test.bat` si no hay
Maven instalado).

---

## 4. Taller de Pruebas de Seguridad (70%)

**¿Qué se hizo, en plata?** Dos cosas complementarias:
1. Una **herramienta automática** (SpotBugs + un plugin llamado FindSecBugs) que
   escanea el código buscando patrones conocidos de vulnerabilidades — como un
   antivirus, pero para código.
2. Una **revisión manual** guiada por el "OWASP Top 10" (una lista estándar de la
   industria con los errores de seguridad más comunes en aplicaciones).

**El hallazgo más importante (¡este te lo van a preguntar seguro!):**
Había un bug real: el sistema de sesión (`Session`) guardaba una referencia
directa al usuario logueado. Si alguien lograra ejecutar código dentro de la
aplicación (por ejemplo, modificando el `.jar`), podía cambiarse el rol a
"ADMINISTRADOR" **sin volver a poner una contraseña**, porque tenía acceso al
mismo objeto que el sistema usa para decidir permisos.

**¿Cómo se arregló?** En vez de entregar el objeto real, `Session` ahora entrega
siempre una **copia**. Así, aunque alguien modifique esa copia, el sistema
original (el que decide si sos admin o no) no se entera y sigue intacto.

**¿Cómo se probó que el arreglo funciona?** Hay un test
(`SessionPrivilegeEscalationTest`) que simula el ataque: inicia sesión como
empleado, intenta cambiarse el rol a administrador con la copia, y confirma que
sigue sin ser administrador. Si alguien rompe el arreglo sin querer en el futuro,
ese test fallaría y avisaría.

**Otras preguntas típicas:**
- "¿Es vulnerable a inyección SQL?" → No. Todo el código usa `PreparedStatement`
  (consultas con parámetros `?`), nunca se arma un SQL pegando texto del usuario.
  Se probó explícitamente mandando textos como `' OR '1'='1` a los buscadores y
  no rompió nada.
- "¿Qué falta por mejorar?" → Cosas que se detectaron pero se dejaron como
  observación (no se arreglaron porque no eran urgentes para el alcance del
  curso): no hay bloqueo de cuenta tras varios intentos fallidos de login, el
  hash de contraseña (SHA-256) es bueno pero no el ideal para contraseñas
  (existen algoritmos más lentos a propósito, como PBKDF2, pensados para eso),
  la base SQLite no está cifrada en disco. Todo esto está en
  `SECURITY_TESTING.md` con su severidad.

---

## 5. Taller de Despliegue (80%)

**¿Qué es Maven y para qué sirve?** Antes, para compilar el proyecto había que
descargar cada librería a mano y correr `javac` con rutas larguísimas. Maven es
una herramienta estándar que lee un archivo (`pom.xml`) donde dices qué
librerías necesitás, y él se encarga de descargarlas y compilar todo. Ventaja
extra: no hace falta tener Maven instalado, porque el proyecto trae un "Maven
Wrapper" (`mvnw.cmd`) que lo descarga solo la primera vez.

**¿Qué significa "configurar un servidor" acá?** El enunciado original pedía
MySQL/Oracle. Para demostrar eso de verdad (no solo decirlo), se levantó un
servidor **MySQL real** con Docker (`docker-compose.yml`) y se hizo que la app se
pudiera conectar a él en vez de a SQLite, **sin cambiar una línea de código** —
solo definiendo tres variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`)
antes de arrancar la app. Esto se pudo hacer así porque desde el principio el
proyecto usa JDBC (el mismo lenguaje para hablar con cualquier base de datos).

**Pregunta típica: "¿Probaste que funciona de verdad contra el servidor?"**
Sí — inicié sesión y registré una venta con la app apuntando al MySQL real (no
solo a SQLite), y confirmé directamente en las tablas de MySQL que el stock se
descontó bien. No fue solo "compila", fue "funciona igual contra un servidor
real".

**Una anécdota útil si preguntan "¿tuviste algún problema al desplegar?":**
Sí, uno real: al armar el `.jar` final con todas las dependencias adentro, dos
librerías (el driver de SQLite y el de MySQL) competían por el mismo archivo
interno que le dice a Java "yo soy un driver de base de datos". Sin arreglarlo,
solo uno de los dos quedaba registrado y el otro dejaba de funcionar. Se
solucionó agregando una configuración (`ServicesResourceTransformer`) que combina
ambos en vez de que uno pise al otro. Es un ejemplo perfecto de "observación
levantada y aplicada para mejorar el despliegue", que es literalmente lo que pide
la rúbrica.

---

## 6. Taller de Monitoreo (90%)

**¿Qué son los "logs"?** Un registro (como una bitácora) de todo lo importante
que hace la app: quién inició sesión, qué ventas se hicieron, qué errores
pasaron. Antes esto no existía; ahora queda guardado en un archivo
(`logs/tiendas-mass.log`) que se puede revisar después si algo salió mal.

**¿Qué es el "chequeo de salud"?** La app se auto-revisa cada 5 minutos: ¿puede
conectarse a la base de datos? ¿cuánta memoria está usando? Si la memoria pasa
el 85%, queda marcado como advertencia en un archivo separado
(`logs/health.log`) — la idea es poder detectar un problema antes de que la app
se caiga, no solo enterarse después.

**¿Y "herramientas de rendimiento"?** Se midió cuánto tarda la operación más
importante (registrar una venta) y queda anotado en el log. Además, se documentó
que existen herramientas que vienen con Java (JConsole, VisualVM) para mirar en
vivo el uso de memoria e hilos de la app mientras corre, sin instalar nada
adicional.

---

## 7. Taller de Mantenimiento (100%)

**¿Qué es un "cron job"?** Es un término de Linux para "una tarea que se
ejecuta sola, en un horario fijo, sin que nadie la dispare a mano". Windows no
tiene cron, tiene su propio equivalente: el **Programador de tareas**. Se
registró ahí una tarea que corre el backup automáticamente **todos los días a
las 11pm**.

**¿Qué hace el backup?** Copia la base de datos (SQLite o, si se está usando el
servidor, un volcado de MySQL) a una carpeta `backups/`, con fecha y hora en el
nombre, y borra solo los que tienen más de 30 días (para no llenar el disco).

**¿Y si algo sale mal y hay que restaurar un backup?** Hay un script para eso
también, que primero guarda una copia de lo que hay *en ese momento* antes de
reemplazarlo — o sea, restaurar tampoco es un riesgo, porque queda un respaldo
de auditoría de "cómo estaba antes de restaurar".

**Prueba real que se hizo (buena para mencionar si preguntan "¿probaste que el
backup sirve?"):** se cambió a propósito un valor en la base de datos (un stock
a un número raro), se restauró desde un backup anterior, y se confirmó que el
valor volvió exactamente al original. No se probó solo que el script "corriera
sin error" — se probó que el dato realmente vuelve a estar bien.

**Si la clase mencionó 4 tipos de scripts de mantenimiento** (Administración,
Backup, Monitoreo, Automatización de Procesos), acá está cada uno:
- **Backup** → `scripts/backup.bat`.
- **Automatización de procesos** → la tarea programada diaria
  (`instalar-tarea-backup.bat`) — corre sola, sin que nadie la dispare a mano.
- **Monitoreo** → `scripts/monitoreo.bat`: a diferencia del chequeo de salud que
  vive dentro de la app (Taller de Monitoreo), este es un script aparte que
  revisa el servidor MySQL, el espacio en disco, y si hay errores recientes en
  el log — **sin necesitar que la app esté corriendo**.
- **Administración** → `scripts/administracion.bat`: prende/apaga el servidor
  MySQL (`iniciar-servidor`/`detener-servidor`) y guarda la configuración de
  conexión (`configurar-entorno`) — o sea, gestiona un recurso del sistema
  (el contenedor) y su configuración, que es literalmente la definición de
  "script de administración".
