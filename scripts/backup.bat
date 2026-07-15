@echo off
REM Backup de la base de datos (SQLite local o el servidor MySQL de docker-compose.yml).
REM Pensado para correr solo (doble clic) o via una tarea programada (ver
REM instalar-tarea-backup.bat). Ver MAINTENANCE.md para el plan completo.
setlocal enabledelayedexpansion
chcp 65001 >nul

cd /d "%~dp0\.."

set BACKUP_DIR=backups
set RETENCION_DIAS=30
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TS=%%i

REM cmd no distingue "variable vacia" de "no definida"; sin este valor por defecto,
REM la comparacion de subcadena de mas abajo puede romper el parser.
if not defined DB_URL set DB_URL=sqlite-local

if "%DB_URL:~0,10%"=="jdbc:mysql" goto backup_mysql
goto backup_sqlite

:backup_mysql
if not defined DB_USER set DB_USER=tiendasmass
if not defined DB_PASSWORD set DB_PASSWORD=tiendasmass_pw
set ARCHIVO=%BACKUP_DIR%\mysql_tiendas_mass_%TS%.sql
echo Respaldando servidor MySQL (docker: tiendasmass-mysql) a %ARCHIVO%...
docker exec tiendasmass-mysql mysqldump -u%DB_USER% -p%DB_PASSWORD% tiendas_mass > "%ARCHIVO%"
if errorlevel 1 goto backup_fallo
goto backup_ok

:backup_sqlite
if exist "tiendas_mass.db" goto backup_sqlite_copiar
echo No se encontro tiendas_mass.db - nada que respaldar.
exit /b 1

:backup_sqlite_copiar
set ARCHIVO=%BACKUP_DIR%\tiendas_mass_%TS%.db
echo Respaldando SQLite local a %ARCHIVO%...
copy /y "tiendas_mass.db" "%ARCHIVO%" >nul
if errorlevel 1 goto backup_fallo
goto backup_ok

:backup_fallo
echo FALLO el backup.
echo %date% %time% FALLO backup >> "%BACKUP_DIR%\backup.log"
exit /b 1

:backup_ok
echo %date% %time% OK %ARCHIVO% >> "%BACKUP_DIR%\backup.log"
echo Backup OK: %ARCHIVO%

echo Limpiando backups de mas de %RETENCION_DIAS% dias...
forfiles /p "%BACKUP_DIR%" /m *.db /d -%RETENCION_DIAS% /c "cmd /c del @path" >nul 2>nul
forfiles /p "%BACKUP_DIR%" /m *.sql /d -%RETENCION_DIAS% /c "cmd /c del @path" >nul 2>nul

exit /b 0
