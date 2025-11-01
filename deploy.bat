@echo off
setlocal

rem Path to Tomcat
set TOMCAT_HOME=E:\apache-tomcat-10.1.34

rem WAR file path
set WAR_FILE=target\akip17-1.0-SNAPSHOT.war

rem Webapp name
set WEBAPP_NAME=akip17

rem Stop Tomcat if running
echo Stopping Tomcat...
call "%TOMCAT_HOME%\bin\shutdown.bat"

rem Wait a bit
timeout /t 5 /nobreak > nul

rem Remove existing webapp if exists
if exist "%TOMCAT_HOME%\webapps\%WEBAPP_NAME%" (
    echo Removing existing webapp...
    rmdir /s /q "%TOMCAT_HOME%\webapps\%WEBAPP_NAME%"
)

rem Copy WAR file to webapps
echo Deploying WAR file...
copy "%WAR_FILE%" "%TOMCAT_HOME%\webapps\%WEBAPP_NAME%.war"

rem Start Tomcat
echo Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

echo Deployment complete. Access the app at http://localhost:8080/%WEBAPP_NAME%
