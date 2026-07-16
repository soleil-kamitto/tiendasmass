@echo off
REM Script de monitoreo INDEPENDIENTE de la app (no requiere que este corriendo):
REM chequea el servidor MySQL (si se usa), espacio en disco y errores recientes
REM en el log de la aplicacion. Complementa a util.HealthCheck (que corre DENTRO
REM de la app cada 5 min, ver MONITORING.md). Pensado para correr solo o via
REM tarea programada, igual que backup.bat. Ver MAINTENANCE.md.
setlocal enabledelayedexpansion
chcp 65001 >nul

cd /d "%~dp0\.."
if not exist "logs" mkdir logs
set REPORTE=logs\monitoreo-externo.log

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TS=%%i

echo ===== Monitoreo %TS% ===== >> "%REPORTE%"
echo ===== Monitoreo %TS% =====

REM --- 1. Servidor MySQL (docker-compose.yml), si se esta usando ---
set CONTENEDOR=
for /f %%i in ('docker ps -q -f "name=tiendasmass-mysql" -f "status=running" 2^>nul') do set CONTENEDOR=%%i
if "%CONTENEDOR%"=="" (
    echo [%TS%] MySQL: contenedor no esta corriendo ^(normal si usas solo SQLite^) >> "%REPORTE%"
    echo MySQL: no esta corriendo ^(normal si usas solo SQLite^)
) else (
    echo [%TS%] MySQL: contenedor tiendasmass-mysql activo >> "%REPORTE%"
    echo MySQL: activo
)

REM --- 2. Espacio en disco de la unidad donde vive el proyecto ---
set UNIDAD=%CD:~0,2%
set DISCO_LIBRE_GB=0
for /f %%i in ('powershell -NoProfile -Command "[math]::Floor((Get-PSDrive -Name '%UNIDAD:~0,1%').Free / 1GB)"') do set DISCO_LIBRE_GB=%%i
echo [%TS%] Espacio libre en %UNIDAD%: %DISCO_LIBRE_GB% GB >> "%REPORTE%"
echo Espacio libre en %UNIDAD%: %DISCO_LIBRE_GB% GB
if %DISCO_LIBRE_GB% LSS 2 (
    echo [%TS%] ADVERTENCIA: menos de 2 GB libres >> "%REPORTE%"
    echo ADVERTENCIA: menos de 2 GB libres
)

REM --- 3. Errores recientes en el log de la aplicacion ---
if exist "logs\tiendas-mass.log" (
    findstr /c:"ERROR" "logs\tiendas-mass.log" >nul
    if errorlevel 1 (
        echo [%TS%] App: sin errores registrados en logs\tiendas-mass.log >> "%REPORTE%"
        echo App: sin errores registrados
    ) else (
        echo [%TS%] App: HAY errores registrados en logs\tiendas-mass.log ^(revisar^) >> "%REPORTE%"
        echo App: HAY errores registrados en logs\tiendas-mass.log - revisar
    )
) else (
    echo [%TS%] App: logs\tiendas-mass.log no existe todavia ^(la app no ha corrido^) >> "%REPORTE%"
    echo App: logs\tiendas-mass.log no existe todavia
)

echo. >> "%REPORTE%"
echo Reporte completo en %REPORTE%
exit /b 0
