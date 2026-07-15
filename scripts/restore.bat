@echo off
REM Restaura un backup generado por backup.bat. Uso:
REM   restore.bat                            -> usa el backup mas reciente en backups\
REM   restore.bat backups\tiendas_mass_...db -> usa el archivo indicado
REM Antes de sobreescribir, guarda una copia de seguridad de lo que hay actualmente
REM (pre-restore_<timestamp>) para que esta operacion tambien sea reversible.
setlocal enabledelayedexpansion
chcp 65001 >nul

cd /d "%~dp0\.."
set BACKUP_DIR=backups

set ARCHIVO=%~1
if not "%ARCHIVO%"=="" goto validar_archivo

for /f "delims=" %%f in ('dir /b /o-d "%BACKUP_DIR%\tiendas_mass_*.db" "%BACKUP_DIR%\mysql_tiendas_mass_*.sql" 2^>nul') do (
    if "!ARCHIVO!"=="" set ARCHIVO=%BACKUP_DIR%\%%f
)
if "%ARCHIVO%"=="" (
    echo No hay backups en %BACKUP_DIR%\ y no se indico un archivo. Nada que restaurar.
    exit /b 1
)

:validar_archivo
if not exist "%ARCHIVO%" (
    echo No existe el archivo: %ARCHIVO%
    exit /b 1
)

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TS=%%i

echo Se va a restaurar: %ARCHIVO%
set /p CONFIRMAR="Esto reemplaza los datos actuales (se guarda una copia de seguridad antes). Continuar? (s/n): "
if /i not "%CONFIRMAR%"=="s" (
    echo Cancelado.
    exit /b 0
)

if /i "%ARCHIVO:~-4%"==".sql" goto restaurar_mysql
goto restaurar_sqlite

REM NOTA: la rama SQLite va antes que la de MySQL a proposito. Con el orden
REM inverso, "goto restaurar_sqlite" tenia que saltar por encima del bloque de
REM MySQL (que usa "docker exec -i ... | ..."), y eso rompia la resolucion de
REM la etiqueta en cmd.exe (probado; ver MAINTENANCE.md).

:restaurar_sqlite
if not exist "tiendas_mass.db" goto restaurar_sqlite_copiar
echo Copia de seguridad previa: %BACKUP_DIR%\pre-restore_%TS%.db
copy /y "tiendas_mass.db" "%BACKUP_DIR%\pre-restore_%TS%.db" >nul

:restaurar_sqlite_copiar
echo Restaurando SQLite desde %ARCHIVO%...
copy /y "%ARCHIVO%" "tiendas_mass.db" >nul
if errorlevel 1 goto restaurar_fallo
goto restaurar_ok

:restaurar_mysql
if not defined DB_USER set DB_USER=tiendasmass
if not defined DB_PASSWORD set DB_PASSWORD=tiendasmass_pw
echo Copia de seguridad previa: %BACKUP_DIR%\pre-restore_%TS%.sql
docker exec tiendasmass-mysql mysqldump -u%DB_USER% -p%DB_PASSWORD% tiendas_mass > "%BACKUP_DIR%\pre-restore_%TS%.sql" 2>nul
echo Restaurando MySQL desde %ARCHIVO%...
docker exec -i tiendasmass-mysql mysql -u%DB_USER% -p%DB_PASSWORD% tiendas_mass < "%ARCHIVO%"
if errorlevel 1 goto restaurar_fallo
goto restaurar_ok

:restaurar_fallo
echo FALLO la restauracion.
echo %date% %time% FALLO restore desde %ARCHIVO% >> "%BACKUP_DIR%\backup.log"
exit /b 1

:restaurar_ok
echo %date% %time% RESTORE desde %ARCHIVO% >> "%BACKUP_DIR%\backup.log"
echo Restauracion OK.
exit /b 0
