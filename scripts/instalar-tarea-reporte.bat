@echo off
REM Registra una tarea programada de Windows que genera el reporte diario de
REM stock todos los dias a las 7:00 am. Requiere permisos de administrador.
setlocal

set TASK_NAME=TiendasMassReporteStockDiario
set SCRIPT_PATH=%~dp0reporte-stock-diario.bat

schtasks /create /tn "%TASK_NAME%" /tr "\"%SCRIPT_PATH%\"" /sc daily /st 07:00 /f
if errorlevel 1 (
    echo No se pudo crear la tarea programada. Ejecuta este script como administrador.
    pause
    exit /b 1
)

echo.
echo Tarea "%TASK_NAME%" creada: corre %SCRIPT_PATH% todos los dias a las 07:00.
echo Para verla: schtasks /query /tn "%TASK_NAME%"
echo Para quitarla: desinstalar-tarea-reporte.bat
pause
