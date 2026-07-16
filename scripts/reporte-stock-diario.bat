@echo off
REM Genera el reporte diario de stock (reportes\stock_<fecha>.txt) usando el
REM ProductoDAO de la app - mismo dato que se ve en Productos/Inventario, sin
REM abrir la interfaz. Pensado para correr solo o via tarea programada (ver
REM instalar-tarea-reporte.bat). Ver MAINTENANCE.md.
setlocal enabledelayedexpansion
chcp 65001 >nul

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

cd /d "%~dp0\.."

if exist "target\tiendas-mass.jar" (
    %JAVA_EXE% -cp target\tiendas-mass.jar com.tiendasmass.inventario.ReporteStock
) else if exist "tiendas-mass.jar" (
    %JAVA_EXE% -cp tiendas-mass.jar com.tiendasmass.inventario.ReporteStock
) else (
    echo No se encontro tiendas-mass.jar. Ejecuta "mvnw.cmd clean package" o "build.bat" primero.
    pause
    exit /b 1
)
