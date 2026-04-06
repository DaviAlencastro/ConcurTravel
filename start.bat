@echo off
cd "c:\Users\Davi\Downloads\Vaga Concur\ConcurTravel"
java -Dspring.profiles.active=local -jar target\travel-policy-service-1.0.0.jar
pause