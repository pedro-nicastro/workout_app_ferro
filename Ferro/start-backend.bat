@echo off
setlocal
cd /d "%~dp0backend"

echo ========================================
echo FERRO - Backend Java + MariaDB
echo ========================================
echo.

where java >nul 2>&1
if errorlevel 1 (
  echo [ERRO] Java nao foi encontrado no PATH.
  echo Instale o JDK 17 ou superior e reabra o terminal.
  pause
  exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
  echo [ERRO] Maven nao foi encontrado no PATH.
  echo Instale o Maven e adicione a pasta bin ao PATH.
  echo.
  echo Depois rode novamente este arquivo.
  pause
  exit /b 1
)

echo [OK] Java encontrado.
echo [OK] Maven encontrado.
echo.
echo Iniciando API em http://127.0.0.1:8081
echo Teste no navegador: http://127.0.0.1:8081/api/health
echo.

call mvn spring-boot:run

endlocal
