@echo off
REM Ejecuta la app contra el servidor MySQL (docker-compose.yml) en vez de SQLite local.
REM Ver DEPLOYMENT.md para el detalle completo del Taller de Despliegue.
setlocal

set JAVA_EXE=java
where java >nul 2>nul
if errorlevel 1 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" (
        set JAVA_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"
    ) else (
        echo No se encontro Java. Instala un JDK 17+ y vuelve a intentarlo.
        pause
        exit /b 1
    )
)

cd /d "%~dp0"
if not exist "target\tiendas-mass.jar" (
    echo No se encontro target\tiendas-mass.jar. Ejecuta "mvnw.cmd clean package" primero.
    pause
    exit /b 1
)

set DB_URL=jdbc:mysql://127.0.0.1:3306/tiendas_mass?allowPublicKeyRetrieval=true^&useSSL=false
set DB_USER=tiendasmass
set DB_PASSWORD=tiendasmass_pw

echo Conectando a %DB_URL% (usuario %DB_USER%)...
%JAVA_EXE% -jar target\tiendas-mass.jar
pause
