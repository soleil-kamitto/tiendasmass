@echo off
setlocal

set JAVA_EXE=java
where java >nul 2>nul
if errorlevel 1 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" (
        set JAVA_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"
    ) else (
        echo No se encontro Java en el PATH ni en la ruta esperada de Temurin 17.
        echo Instala un JDK 17+ ^(por ejemplo, Eclipse Temurin^) y vuelve a intentarlo.
        pause
        exit /b 1
    )
)

if not exist "%~dp0tiendas-mass.jar" (
    echo No se encontro tiendas-mass.jar. Ejecuta build.bat primero.
    pause
    exit /b 1
)

cd /d "%~dp0"
%JAVA_EXE% -jar tiendas-mass.jar
pause
