@echo off
setlocal enabledelayedexpansion
title FinLink AI - Gerador de APK Assinado
cls

echo =======================================================================
echo              FinLink AI - Gerador de APK Assinado
echo =======================================================================
echo.

REM 1. Deteccao do Java JDK / JBR
if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    ) else if exist "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8"
    )
)

if defined JAVA_HOME (
    echo [OK] JDK configurado: "%JAVA_HOME%"
)

REM 2. Deteccao do Android SDK
if not defined ANDROID_HOME (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    )
)

if defined ANDROID_HOME (
    echo [OK] Android SDK configurado: "%ANDROID_HOME%"
)

echo.
echo Iniciando compilacao do APK Assinado...
echo Aguarde alguns instantes...
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Ocorreu uma falha na compilacao! Verifique as mensagens acima.
    pause
    exit /b 1
)

if not exist "APK_Gerado" mkdir "APK_Gerado"

copy /y "app\build\outputs\apk\debug\app-debug.apk" "APK_Gerado\FinLink_AI_Assinado.apk" >nul

echo.
echo =======================================================================
echo [SUCESSO] APK Assinado gerado com sucesso!
echo Localizacao:
echo   %CD%\APK_Gerado\FinLink_AI_Assinado.apk
echo =======================================================================
echo.
echo Abrindo a pasta do arquivo APK no Windows Explorer...
explorer "%CD%\APK_Gerado"

pause
endlocal
