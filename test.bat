@echo off
setlocal enabledelayedexpansion

set JAVAC_EXE=javac
set JAVA_EXE=java
where javac >nul 2>nul
if errorlevel 1 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe" (
        set JAVAC_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe"
        set JAVA_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"
    ) else (
        echo No se encontro un JDK 17+. Instala Eclipse Temurin 17 y vuelve a intentarlo.
        pause
        exit /b 1
    )
)

cd /d "%~dp0"
if exist out-test rmdir /s /q out-test
mkdir out-test

set CP=lib\sqlite-jdbc-3.46.1.3.jar;lib\flatlaf-3.5.4.jar;lib\junit-platform-console-standalone-1.10.3.jar

echo Compilando fuentes principales y de test...
dir /s /b src\main\java\*.java src\test\java\*.java > sources.txt
%JAVAC_EXE% -encoding UTF-8 -cp "%CP%" -d out-test @sources.txt
if errorlevel 1 (
    echo La compilacion fallo.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

echo.
echo Ejecutando pruebas...
%JAVA_EXE% -jar lib\junit-platform-console-standalone-1.10.3.jar execute ^
    -cp out-test;lib\sqlite-jdbc-3.46.1.3.jar;lib\flatlaf-3.5.4.jar ^
    --scan-class-path out-test ^
    --reports-dir=test-reports ^
    --details=tree

set EXIT_CODE=%errorlevel%
echo.
echo Reporte XML (JUnit) generado en test-reports\
pause
exit /b %EXIT_CODE%
