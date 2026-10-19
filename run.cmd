@echo off
rem Starts MetroPulse with its bundled PostgreSQL database.
rem Works from any folder: it always switches to the project folder (where pom.xml is) first.
cd /d "%~dp0"
mvn spring-boot:test-run -Dspring-boot.run.main-class=com.metropulse.LocalDevApplication
