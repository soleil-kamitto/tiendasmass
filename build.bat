@echo off
setlocal enabledelayedexpansion

set JAVAC_EXE=javac
where javac >nul 2>nul
if errorlevel 1 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe" (
        set JAVAC_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe"
        set JAR_EXE="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\jar.exe"
    ) else (
        echo No se encontro un JDK 17+. Instala Eclipse Temurin 17 y vuelve a intentarlo.
        pause
        exit /b 1
    )
) else (
    set JAR_EXE=jar
)

cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out

set CP=lib\sqlite-jdbc-3.46.1.3.jar;lib\flatlaf-3.5.4.jar;lib\slf4j-api-2.0.16.jar;lib\logback-classic-1.5.8.jar;lib\logback-core-1.5.8.jar

echo Compilando fuentes...
dir /s /b src\main\java\*.java > sources.txt
%JAVAC_EXE% -encoding UTF-8 -cp "%CP%" -d out @sources.txt
if errorlevel 1 (
    echo La compilacion fallo.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

echo Empaquetando dependencias (SQLite JDBC, FlatLaf, SLF4J/Logback) dentro de out...
REM Nota: este build no incluye mysql-connector-j (servidor MySQL del Taller de
REM Despliegue) porque el jar resultante compartiria META-INF/services/java.sql.Driver
REM entre dos drivers JDBC y uno pisaria al otro sin un merge como el que hace
REM maven-shade-plugin (ver DEPLOYMENT.md). Para desplegar contra MySQL usa
REM "mvnw.cmd clean package" en vez de build.bat.
if exist unpack rmdir /s /q unpack
mkdir unpack
pushd unpack
%JAR_EXE% xf "..\lib\sqlite-jdbc-3.46.1.3.jar"
%JAR_EXE% xf "..\lib\flatlaf-3.5.4.jar"
%JAR_EXE% xf "..\lib\slf4j-api-2.0.16.jar"
%JAR_EXE% xf "..\lib\logback-classic-1.5.8.jar"
%JAR_EXE% xf "..\lib\logback-core-1.5.8.jar"
popd
xcopy /e /i /y unpack\org out\org >nul
xcopy /e /i /y unpack\com out\com >nul
xcopy /e /i /y unpack\ch out\ch >nul
xcopy /e /i /y unpack\META-INF out\META-INF >nul
copy /y src\main\resources\logback.xml out\logback.xml >nul

echo Manifest-Version: 1.0> out\manifest.txt
echo Main-Class: com.tiendasmass.inventario.App>> out\manifest.txt
echo.>> out\manifest.txt

if exist tiendas-mass.jar del tiendas-mass.jar
%JAR_EXE% cfm tiendas-mass.jar out\manifest.txt -C out .

rmdir /s /q unpack

echo.
echo Listo: tiendas-mass.jar generado. Ejecuta run.bat para iniciar el sistema.
pause
