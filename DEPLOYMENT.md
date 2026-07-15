# Despliegue — Tiendas Mass (Taller de Despliegue)

Este documento cubre el build con **Maven** y la **configuración de un servidor**
(MySQL) para que la aplicación pueda correr contra un motor cliente-servidor, tal
como proponía originalmente el enunciado del proyecto antes de optar por SQLite
local (ver "Decisión técnica" en `README.md`).

## 1. Build con Maven

El proyecto se migró de scripts `javac`/`jar` a mano (`build.bat`, que se conserva
por compatibilidad) a un `pom.xml` estándar. No hace falta tener Maven instalado:
el repo incluye el **Maven Wrapper** (`mvnw` / `mvnw.cmd`), que descarga la versión
correcta de Maven la primera vez que se ejecuta.

```bat
mvnw.cmd test              REM compila y corre toda la suite (19 tests)
mvnw.cmd clean package      REM genera target\tiendas-mass.jar (jar ejecutable con todas las dependencias)
```

`pom.xml` declara las dependencias (`sqlite-jdbc`, `mysql-connector-j`, `flatlaf`,
`junit-jupiter`), el compilador en Java 17, Surefire para correr los tests JUnit 5, y
`maven-shade-plugin` para empaquetar un jar ejecutable equivalente al que antes
armaba `build.bat` manualmente.

## 2. Configuración del servidor (MySQL)

`Database.java` sigue usando SQLite local por defecto (para no romper el diseño de
"Alternativa 1" ya sustentado: seguir funcionando sin red). Pero ahora lee tres
variables de entorno opcionales — **sin tocar código** — para conectarse en su lugar
a un servidor MySQL:

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/tiendas_mass?allowPublicKeyRetrieval=true&useSSL=false` |
| `DB_USER` | `tiendasmass` |
| `DB_PASSWORD` | `tiendasmass_pw` |

Si `DB_URL` no está definida, la app usa SQLite exactamente como antes. Si sí lo
está y empieza con `jdbc:mysql:`, `Database` crea el mismo esquema (adaptado al
dialecto MySQL: `AUTO_INCREMENT`, `VARCHAR`/`DECIMAL` en vez de `TEXT`/`REAL`,
claves foráneas explícitas) y siembra los mismos datos de prueba.

### Levantar el servidor localmente

Se incluye `docker-compose.yml` con un MySQL 8.4 listo para usar (credenciales de
demo, igual que `admin/admin123` ya documentado para la app):

```bat
docker compose up -d      REM levanta el servidor MySQL en localhost:3306
run-server.bat            REM corre target\tiendas-mass.jar apuntando a ese servidor
```

`run-server.bat` simplemente exporta `DB_URL`/`DB_USER`/`DB_PASSWORD` antes de
lanzar el jar — es la única diferencia con `run.bat` (que sigue usando SQLite).

> Para un despliegue real (no de demo/curso) el servidor MySQL debería vivir en su
> propia infraestructura (on-prem o en la nube) con credenciales gestionadas por
> fuera del repo; aquí se usa Docker local para que el despliegue sea reproducible
> por cualquiera que revise el proyecto sin depender de servicios de terceros.

## 3. Verificación realizada

Se verificó el despliegue de punta a punta contra el servidor real (no solo que
compile): con el MySQL de `docker-compose.yml` corriendo, se ejecutó la app
(`Database.initSchema()`, `AuthService.login()`, `VentaService.registrarVenta()`) y
se confirmó directamente en MySQL que:

- El esquema y los datos semilla se crearon correctamente en dialecto MySQL.
- El login (`admin`/`admin123`) autenticó igual que contra SQLite.
- Una venta de prueba se registró, descontó el stock del producto (40 → 38 unidades
  para 2 unidades vendidas) y quedó consistente entre `ventas` y `detalle_venta` —
  la misma transacción JDBC (`VentaService`) funciona sin cambios contra el servidor.

## 4. Observaciones levantadas y aplicadas

- **Jar sombreado con dos drivers JDBC en conflicto.** Al armar el jar con
  `maven-shade-plugin`, tanto `sqlite-jdbc` como `mysql-connector-j` traen un
  archivo `META-INF/services/java.sql.Driver` (el mecanismo de
  autoregistro de `DriverManager`). Por defecto, el plugin sobrescribe el archivo de
  una dependencia con el de la otra en vez de combinarlos — se comprobó
  extrayendo ese archivo del jar generado y solo aparecía `org.sqlite.JDBC`, sin el
  driver de MySQL. Esto habría hecho fallar cualquier conexión a
  `jdbc:mysql:...` en el jar empaquetado (aunque `mvn test` sí funcionara, porque
  ahí las dependencias están en classpaths separados, no fusionadas). Se corrigió
  agregando el `ServicesResourceTransformer` en `pom.xml`, que fusiona los
  archivos de servicios en vez de pisarlos; se volvió a generar el jar y se
  confirmó que ambos drivers (`org.sqlite.JDBC` y `com.mysql.cj.jdbc.Driver`)
  quedan registrados.
- **No commitear el jar de Maven Wrapper ni credenciales reales.** Se usó el
  Maven Wrapper oficial (jar + scripts de `apache/maven-wrapper`) para no
  depender de que el evaluador tenga Maven preinstalado, y las credenciales de
  `docker-compose.yml`/`run-server.bat` son explícitamente de demo local (mismo
  criterio que las credenciales de prueba ya documentadas en `README.md`), no
  pensadas para un entorno productivo real.
