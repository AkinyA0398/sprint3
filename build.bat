@echo off
setlocal

rem Directories
set SRC_DIR=src
set LIB_DIR=lib
set CLASSES_DIR=classes
set WEBAPP_DIR=src/main/webapp
set TARGET_DIR=target

rem Create classes directory if not exists
if not exist "%CLASSES_DIR%" (
    mkdir "%CLASSES_DIR%"
)

rem Compile Java files
echo Compiling Java files...
dir /S /B %SRC_DIR%\*.java > sources.txt
javac -cp "%LIB_DIR%\*" -d "%CLASSES_DIR%" @sources.txt

rem Create WAR file
echo Creating WAR file...
if not exist "%TARGET_DIR%" (
    mkdir "%TARGET_DIR%"
)

cd "%WEBAPP_DIR%"
jar cvf "../../%TARGET_DIR%/akip17-1.0-SNAPSHOT.war" *
cd ../..

rem Add classes to WAR
cd "%CLASSES_DIR%"
jar uvf "../%TARGET_DIR%/akip17-1.0-SNAPSHOT.war" *
cd ..

echo Build complete.
