@echo off
REM Script de administracion: gestiona recursos (el contenedor MySQL) y
REM configuraciones (variables de entorno DB_URL/DB_USER/DB_PASSWORD) del
REM sistema operativo para esta app. Ver MAINTENANCE.md.
REM Uso:
REM   administracion.bat estado               -> estado del servidor MySQL y de Java
REM   administracion.bat iniciar-servidor      -> levanta el MySQL de docker-compose.yml
REM   administracion.bat detener-servidor      -> lo detiene
REM   administracion.bat configurar-entorno    -> guarda DB_URL/DB_USER/DB_PASSWORD como
REM                                                variables de entorno persistentes del usuario
REM
REM NOTA: cada accion es un bloque "if ... (...) exit /b" independiente, a
REM proposito, en vez de goto/etiquetas: un goto que tuviera que saltar por
REM encima de un bloque con un FOR /F sobre "docker ps ... 2>nul" rompia la
REM resolucion de etiquetas de cmd.exe (mismo bug ya documentado en
REM MAINTENANCE.md para restore.bat). Sin goto, el problema no aplica.
setlocal enabledelayedexpansion
chcp 65001 >nul
cd /d "%~dp0\.."

if "%~1"=="estado" (
    echo == Estado del servidor MySQL ==
    set CONTENEDOR=
    for /f %%i in ('docker ps -q -f "name=tiendasmass-mysql" -f "status=running" 2^>nul') do set CONTENEDOR=%%i
    if "!CONTENEDOR!"=="" (
        echo MySQL: no esta corriendo
    ) else (
        echo MySQL: corriendo ^(contenedor !CONTENEDOR!^)
    )
    echo.
    echo == Version de Java ==
    set JAVA_EXE=java
    where java >nul 2>nul
    if errorlevel 1 (
        if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" (
            set JAVA_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"
        ) else (
            set JAVA_EXE=
            echo No se encontro Java en el PATH ni en la ruta estandar de Temurin 17.
        )
    )
    if not "!JAVA_EXE!"=="" !JAVA_EXE! -version
    echo.
    echo == Variables de entorno de conexion actuales ==
    echo DB_URL=%DB_URL%
    echo DB_USER=%DB_USER%
    exit /b 0
)

if "%~1"=="iniciar-servidor" (
    echo Levantando el servidor MySQL ^(docker-compose.yml^)...
    docker compose up -d
    exit /b 0
)

if "%~1"=="detener-servidor" (
    echo Deteniendo el servidor MySQL...
    docker compose down
    exit /b 0
)

if "%~1"=="configurar-entorno" (
    echo Esto guarda DB_URL/DB_USER/DB_PASSWORD como variables de entorno permanentes
    echo del usuario actual de Windows, para no tener que definirlas cada vez.
    set /p DB_USER_NUEVO="Usuario de MySQL [tiendasmass]: "
    if "!DB_USER_NUEVO!"=="" set DB_USER_NUEVO=tiendasmass
    set /p DB_PASSWORD_NUEVO="Password de MySQL [tiendasmass_pw]: "
    if "!DB_PASSWORD_NUEVO!"=="" set DB_PASSWORD_NUEVO=tiendasmass_pw

    setx DB_URL "jdbc:mysql://127.0.0.1:3306/tiendas_mass?allowPublicKeyRetrieval=true&useSSL=false" >nul
    setx DB_USER "!DB_USER_NUEVO!" >nul
    setx DB_PASSWORD "!DB_PASSWORD_NUEVO!" >nul
    echo Listo. Abre una terminal nueva para que tomen efecto ^(setx no afecta la sesion actual^).
    exit /b 0
)

echo Uso: administracion.bat [estado^|iniciar-servidor^|detener-servidor^|configurar-entorno]
exit /b 0
