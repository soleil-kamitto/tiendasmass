@echo off
REM Quita la tarea programada creada por instalar-tarea-reporte.bat.
setlocal

set TASK_NAME=TiendasMassReporteStockDiario

schtasks /delete /tn "%TASK_NAME%" /f
if errorlevel 1 (
    echo No se encontro la tarea "%TASK_NAME%" o no se pudo eliminar.
    pause
    exit /b 1
)

echo Tarea "%TASK_NAME%" eliminada.
pause
