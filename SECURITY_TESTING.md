# Reporte de Pruebas de Seguridad — Tiendas Mass (Taller de Pruebas de Seguridad)

**Fecha:** 2026-07-15
**Alcance:** código fuente en `src/main/java` (aplicación de escritorio Java Swing +
JDBC/SQLite descrita en `README.md`).

## 1. Metodología

Se combinaron dos técnicas complementarias:

1. **Análisis estático (SAST) con herramienta automatizada.**
   [SpotBugs 4.8.6](https://spotbugs.github.io/) + el plugin
   [FindSecBugs 1.13.0](https://find-sec-bugs.github.io/) (detectores específicos de
   seguridad: inyección SQL, criptografía débil, uso inseguro de `Random`, etc.) sobre
   las clases compiladas de `src/main/java` (sin las librerías vendorizadas de
   `lib/`, para no analizar código de terceros). Comando usado:

   ```bat
   java -jar tools\spotbugs-4.8.6\lib\spotbugs.jar -textui ^
       -pluginList tools\findsecbugs-plugin.jar ^
       -auxclasspath "lib\sqlite-jdbc-3.46.1.3.jar;lib\flatlaf-3.5.4.jar" ^
       -low -longBugCodes out-main
   ```

   Reporte completo (texto y XML) en `security-reports/`.

2. **Revisión manual de código + pruebas de seguridad dinámicas (JUnit 5).**
   Revisión guiada por OWASP Top 10 (2021) de los puntos de mayor riesgo: DAOs (acceso
   a datos vía SQL), autenticación (`AuthService`, `PasswordUtil`), y control de sesión
   y roles (`Session`, `MainFrame`). Los hallazgos con impacto real se verificaron con
   pruebas ejecutables en `src/test/java/com/tiendasmass/inventario/security/`
   (`SqlInjectionTest`, `PasswordStorageTest`, `SessionPrivilegeEscalationTest`), que
   corren junto al resto de la suite con `test.bat`.

No se evaluaron aspectos como red/TLS o inyección web (XSS/CSRF/SSRF) porque la
aplicación no expone servicios de red ni interfaz web: toda la superficie de ataque
relevante es el proceso local, sus datos en disco (`tiendas_mass.db`) y el propio
`.jar` distribuido.

## 2. Hallazgos

| # | Hallazgo | Severidad | OWASP | Estado |
|---|---|---|---|---|
| 1 | Escalada de privilegios en memoria vía `Session` | **Alto** | A01:2021 Broken Access Control | **Corregido** |
| 2 | Control de acceso por rol validado solo en la UI, no en el servicio/DAO | Medio-Alto | A01:2021 Broken Access Control | Pendiente (observación) |
| 3 | Hash de contraseña sin factor de trabajo (SHA-256 simple) | Medio | A02:2021 Cryptographic Failures | Pendiente (observación) |
| 4 | Base de datos SQLite sin cifrar en disco | Medio | A02:2021 Cryptographic Failures | Pendiente (observación) |
| 5 | Sin límite de intentos / bloqueo de cuenta en login | Bajo-Medio | A07:2021 Auth. Failures | Pendiente (observación) |
| 6 | `SecureRandom` reinstanciado en cada hash | Bajo | — (buena práctica) | **Corregido** |
| 7 | Contraseña retenida como `String` en memoria (no se limpia el `char[]`) | Informativo | CWE-244 | Pendiente (observación) |
| 8 | Inyección SQL en los DAO | — | A03:2021 Injection | **No vulnerable** (verificado) |
| 9 | Enumeración de usuarios vía mensaje de error de login | — | A07:2021 | **No vulnerable** (verificado) |
| 10 | Contraseñas en texto plano en la base de datos | — | A02:2021 | **No vulnerable** (verificado) |

### Hallazgo 1 — Escalada de privilegios en memoria vía `Session` (Alto, corregido)

**Dónde:** `util/Session.java` (antes de la corrección, líneas 13-19); usado desde
`ui/MainFrame.java:54,75,136` para decidir si se muestra el panel/menú de
"Usuarios" (administración de cuentas y roles).

**Descripción:** `Session.actual()` devolvía la **misma instancia** de `Usuario` que
`Session` guarda internamente para decidir permisos, y `Session.iniciar(Usuario)`
guardaba directamente la referencia que le pasaban. Cualquier código con acceso a
esa referencia podía hacer `Session.actual().setRol(Rol.ADMINISTRADOR)` y elevar sus
propios privilegios sin volver a autenticarse, porque `esAdministrador()` lee ese
mismo objeto mutado. Esto es explotable en este tipo de aplicación porque se instala
y ejecuta localmente en cada caja (ver README): nada impide a alguien decompilar
`tiendas-mass.jar`, insertar una línea que llame a ese setter, y recompilar/parchear
el jar para obtener acceso de administrador sin credenciales.

**Evidencia:** detectado también por SpotBugs (`EI_EXPOSE_REP`, `EI_EXPOSE_REP2`,
`MS_EXPOSE_REP`, `EI_EXPOSE_STATIC_REP2` sobre `Session`/`Usuario`).

**Corrección aplicada:** se agregó un constructor de copia a `Usuario`
(`model/Usuario.java`) y `Session.iniciar()`/`Session.actual()` ahora copian el
objeto al entrar y al salir (`util/Session.java:13-23`), de modo que quien reciba el
usuario de la sesión nunca tiene la referencia real.

**Prueba de regresión:**
`security/SessionPrivilegeEscalationTest.mutarElUsuarioDevueltoPorActualNoEscalaPrivilegiosDeLaSesionReal`
— inicia sesión como `EMPLEADO`, intenta mutar el rol del objeto devuelto por
`actual()` a `ADMINISTRADOR`, y confirma que `esAdministrador()` sigue siendo `false`.

### Hallazgo 2 — Autorización solo en la capa de UI (Medio-Alto, pendiente)

**Dónde:** `ui/MainFrame.java:54,75,136` (único lugar donde se consulta
`Session.esAdministrador()` en toda la aplicación).

**Descripción:** El control de acceso por rol (RF12/RNF02) consiste únicamente en
ocultar el panel y las entradas de menú de "Usuarios" si el usuario en sesión no es
administrador. Ni `UsuarioDAO` ni ningún service verifican el rol antes de ejecutar
`insertar`, `actualizarRolYEstado` o `cambiarPassword`. En un cliente-servidor esto
sería crítico porque un cliente modificado llamaría directamente a la API; aquí, dado
que todo corre en el mismo proceso, el vector concreto es un `.jar` parchado/decompilado
que invoque `UsuarioDAO` directamente sin pasar por la UI.

**Recomendación:** agregar una verificación de rol al inicio de las operaciones
sensibles en `UsuarioDAO`/un futuro `UsuarioService` (defensa en profundidad), en vez
de confiar solo en qué botones se muestran.

### Hallazgo 3 — Hash de contraseña sin factor de trabajo (Medio, pendiente)

**Dónde:** `util/PasswordUtil.java:32-40`.

**Descripción:** `PasswordUtil` usa SHA-256 con salt aleatorio de 16 bytes por
usuario (buena práctica: evita rainbow tables y detecta colisiones entre usuarios) y
compara con `MessageDigest.isEqual` (tiempo constante, evita timing attacks). Sin
embargo, SHA-256 es una función de hash *rápida*, diseñada para integridad, no para
contraseñas: si `tiendas_mass.db` se filtra, los hashes se pueden atacar por fuerza
bruta con hardware GPU a alta velocidad, ya que no hay factor de costo/iteraciones.

**Recomendación:** migrar a `PBKDF2WithHmacSHA256` (disponible en el JDK sin
dependencias nuevas, con un número de iteraciones configurable) o, si se acepta una
dependencia externa, a BCrypt/Argon2.

### Hallazgo 4 — Base de datos sin cifrar en disco (Medio, pendiente)

**Dónde:** `db/Database.java` (URL JDBC sin ninguna opción de cifrado).

**Descripción:** `tiendas_mass.db` se guarda sin cifrar junto al `.jar`. Contiene
hashes de contraseñas, datos de clientes (incluyendo documento/DNI) y el historial
completo de ventas. Dado que el sistema se instala físicamente en cada caja (según el
README, precisamente para tolerar fallas de red), el robo o copia del archivo expone
todos los datos con cualquier cliente SQLite genérico.

**Recomendación:** para un siguiente incremento, evaluar SQLCipher (SQLite con
cifrado AES transparente) si el riesgo de acceso físico no autorizado se considera
relevante para el negocio.

### Hallazgo 5 — Sin límite de intentos de login (Bajo-Medio, pendiente)

**Dónde:** `service/AuthService.java`.

**Descripción:** `login(usuario, password)` no lleva ningún conteo de intentos
fallidos ni aplica retraso/bloqueo. Nada impide reintentar contraseñas
indefinidamente contra un usuario válido.

**Recomendación:** agregar un contador de intentos fallidos por usuario (columna en
`usuarios` o caché en memoria) con bloqueo temporal tras N intentos.

### Hallazgo 6 — `SecureRandom` reinstanciado en cada hash (Bajo, corregido)

**Dónde:** `util/PasswordUtil.java:11,18` (antes: `new SecureRandom()` dentro de
`hash(...)` en cada llamada).

**Descripción:** no es una vulnerabilidad explotable (`SecureRandom` sigue siendo
criptográficamente seguro aunque se cree una instancia nueva cada vez), pero
SpotBugs lo marca (`DMI_RANDOM_USED_ONLY_ONCE`) porque cada instancia nueva vuelve a
sembrarse desde la entropía del sistema, lo cual es innecesariamente costoso.

**Corrección aplicada:** se reutiliza una única instancia `static final SecureRandom`
para todas las llamadas.

### Hallazgo 7 — Contraseña retenida como `String` en memoria (Informativo)

**Dónde:** `ui/LoginFrame.java:126`, `ui/UsuariosPanel.java:84,137`.

**Descripción:** el contenido de `JPasswordField.getPassword()` (un `char[]`, que sí
se puede limpiar explícitamente) se convierte a `String` con
`new String(password.getPassword())`. Los `String` son inmutables en Java y quedan en
el heap hasta que el recolector de basura los retire, por lo que la contraseña en
texto plano puede persistir en memoria más tiempo del necesario (visible, por
ejemplo, en un volcado de memoria). Es un hallazgo menor dado el contexto (aplicación
de escritorio de un solo usuario), pero se documenta como buena práctica pendiente:
operar directamente sobre el `char[]` y sobrescribirlo (`Arrays.fill(pwd, '\0')`)
después de usarlo.

### Hallazgos verificados como NO vulnerables

- **Inyección SQL (#8):** los 10 DAO revisados (`ProductoDAO`, `UsuarioDAO`,
  `ClienteDAO`, `ReclamoDAO`, `VentaDAO`, `MovimientoInventarioDAO`) usan
  `PreparedStatement` con parámetros (`?`) en el 100% de las consultas — ninguna
  concatena texto de usuario dentro del SQL. FindSecBugs no reportó ningún hallazgo
  de inyección, y `security/SqlInjectionTest` confirma dinámicamente que payloads
  clásicos (`' OR '1'='1`, `'; DROP TABLE productos; --`, `admin' -- `) no alteran el
  comportamiento de `ProductoDAO.buscar` ni `UsuarioDAO.buscarPorUsuario`.
- **Enumeración de usuarios (#9):** `ui/LoginFrame.java:136` muestra el mismo mensaje
  genérico ("Usuario o contraseña incorrectos, o cuenta inactiva.") sin importar si
  el usuario no existe, la contraseña es incorrecta o la cuenta está desactivada —
  no se puede distinguir un caso de otro desde la UI.
- **Contraseñas en texto plano (#10):** confirmado por
  `security/PasswordStorageTest`, que lee directamente la fila cruda de
  `usuarios.password_hash` en la base y verifica que nunca contiene la contraseña en
  texto plano y que respeta el formato `salt:hash`.

## 3. Cómo reproducir

```bat
REM Pruebas de seguridad (parte de la suite completa):
test.bat

REM Análisis estático SpotBugs + FindSecBugs (requiere tools/ descargado, ver abajo):
REM 1. Descargar https://github.com/spotbugs/spotbugs/releases/download/4.8.6/spotbugs-4.8.6.zip en tools/ y descomprimir
REM 2. Descargar https://repo1.maven.org/maven2/com/h3xstream/findsecbugs/findsecbugs-plugin/1.13.0/findsecbugs-plugin-1.13.0.jar en tools/
REM 3. Compilar solo src/main/java a out-main/ y correr spotbugs.jar -textui como en la sección 1.
```

## 4. Conclusión

De 10 hallazgos, 2 eran vulnerabilidades reales con corrección inmediata de bajo
riesgo (escalada de privilegios en memoria y una mejora menor de rendimiento
criptográfico) y ya fueron corregidos y cubiertos con pruebas de regresión. 3 se
verificaron como no vulnerables con evidencia (herramienta + pruebas dinámicas). Los
5 restantes son observaciones documentadas — válidas para el contexto de un sistema
de escritorio de una sola tienda, pero que quedan como trabajo futuro porque
requieren cambios de mayor alcance (autorización en la capa de servicio, cambio de
algoritmo de hashing, cifrado de la base de datos, política de bloqueo de cuentas).
