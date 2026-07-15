@echo off
REM Registra una tarea programada de Windows (el equivalente a un cron job) que
REM corre backup.bat todos los dias a las 23:00. Requiere permisos de administrador.
setlocal

set TASK_NAME=TiendasMassBackupDiario
set SCRIPT_PATH=%~dp0backup.bat

schtasks /create /tn "%TASK_NAME%" /tr "\"%SCRIPT_PATH%\"" /sc daily /st 23:00 /f
if errorlevel 1 (
    echo No se pudo crear la tarea programada. Ejecuta este script como administrador.
    pause
    exit /b 1
)

echo.
echo Tarea "%TASK_NAME%" creada: corre %SCRIPT_PATH% todos los dias a las 23:00.
echo Para verla: schtasks /query /tn "%TASK_NAME%"
echo Para quitarla: desinstalar-tarea-backup.bat
pause
