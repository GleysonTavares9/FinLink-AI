@echo off
setlocal enabledelayedexpansion
title FinLink AI - Compilador e Instalador no Celular
cls

echo =======================================================================
echo          FinLink AI - Compilador e Instalador Android
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
    echo [OK] JDK detectado: "!JAVA_HOME!"
) else (
    echo [AVISO] JAVA_HOME nao encontrado. Tentando usar o PATH do sistema...
)

REM 2. Deteccao do Android SDK e ADB
if not defined ANDROID_HOME (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    )
)

set "ADB_PATH=%ANDROID_HOME%\platform-tools\adb.exe"
if exist "%ADB_PATH%" (
    echo [OK] Android SDK e ADB detectados: "%ADB_PATH%"
) else (
    set "ADB_PATH=adb"
)

echo.
echo =======================================================================
echo Escolha a opcao desejada:
echo.
echo  [1] Compilar APK Assinado e Instalar/Abrir no Celular
echo  [2] Apenas Gerar o APK Assinado (sem instalar)
echo  [3] Compilar Versao Release Assinada
echo  [4] Conectar Celular via Wi-Fi (ADB Wireless sem cabo)
echo  [5] Acompanhar Logs do App no Celular (Logcat)
echo  [0] Sair
echo =======================================================================
set "OPTION=1"
set /p OPTION="Digite a opcao (1 a 5, padrao = 1): "

if "%OPTION%"=="0" goto FIM
if "%OPTION%"=="2" goto COMPILAR_APENAS
if "%OPTION%"=="3" goto COMPILAR_RELEASE
if "%OPTION%"=="4" goto WIFI_ADB
if "%OPTION%"=="5" goto LOGCAT

:COMPILAR_E_INSTALAR
echo.
echo -----------------------------------------------------------------------
echo [1/3] Compilando e gerando APK assinado com Gradle...
echo -----------------------------------------------------------------------
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Falha na compilacao do Gradle! Verifique os erros acima.
    pause
    goto FIM
)

echo.
echo -----------------------------------------------------------------------
echo [2/3] Verificando conexao com o celular via Depuracao USB...
echo -----------------------------------------------------------------------
"%ADB_PATH%" devices
echo.

set "DEVICE_FOUND=0"
for /f "skip=1 tokens=1,2" %%A in ('"%ADB_PATH%" devices') do (
    if "%%B"=="device" (
        set "DEVICE_FOUND=1"
    )
)

if "%DEVICE_FOUND%"=="0" (
    echo [AVISO] Nenhum celular autorizado foi detectado via USB!
    echo.
    echo Como ativar no celular:
    echo  1. No celular, va em Configuracoes -^> Sobre o telefone.
    echo  2. Toque 7 vezes em "Numero da versao" ate virar desenvolvedor.
    echo  3. Volte e acesse "Opcoes do desenvolvedor".
    echo  4. Ative a opcao "Depuracao USB".
    echo  5. Conecte o cabo USB e aceite a mensagem no celular.
    echo.
    echo Conecte o cabo USB no celular e pressione qualquer tecla para continuar...
    pause >nul
)

:INSTALAR
echo.
echo -----------------------------------------------------------------------
echo [3/3] Instalando e iniciando o FinLink AI no aparelho...
echo -----------------------------------------------------------------------
set "APK_FILE=app\build\outputs\apk\debug\app-debug.apk"

if not exist "%APK_FILE%" (
    echo [ERRO] Arquivo APK nao encontrado em: %APK_FILE%
    pause
    goto FIM
)

"%ADB_PATH%" install -r -d "%APK_FILE%"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCESSO] App instalado com sucesso! Iniciando tela principal...
    "%ADB_PATH%" shell am start -n com.aistudio.cupomfinancas.app/com.example.MainActivity
    echo.
    echo O aplicativo FinLink AI ja esta aberto no seu celular!
) else (
    echo.
    echo [AVISO] Nao foi possivel instalar automaticamente via ADB.
    echo O APK assinado esta pronto para instalacao manual em:
    echo  %CD%\%APK_FILE%
)

echo.
pause
goto FIM

:COMPILAR_APENAS
echo.
echo Compilando APK Debug Assinado...
call gradlew.bat assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCESSO] APK gerado em:
    echo  %CD%\app\build\outputs\apk\debug\app-debug.apk
    explorer /select,"%CD%\app\build\outputs\apk\debug\app-debug.apk"
)
pause
goto FIM

:COMPILAR_RELEASE
echo.
echo Compilando APK Release...
call gradlew.bat assembleRelease
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCESSO] APK Release gerado em:
    echo  %CD%\app\build\outputs\apk\release
    explorer "%CD%\app\build\outputs\apk\release"
)
pause
goto FIM

:WIFI_ADB
echo.
echo Conecte primeiro o celular no cabo USB para habilitar a porta 5555.
"%ADB_PATH%" tcpip 5555
echo.
set /p IP_PHONE="Digite o IP do celular na rede Wi-Fi (ex: 192.168.1.105): "
"%ADB_PATH%" connect %IP_PHONE%:5555
echo.
echo Conexao Wi-Fi configurada! Agora voce pode desconectar o cabo USB.
pause
goto FIM

:LOGCAT
echo.
echo Exibindo logs do FinLink AI no celular (pressione Ctrl+C para sair)...
"%ADB_PATH%" logcat -s "FinLinkAI:V" "MainActivity:V" "AndroidRuntime:E"
pause
goto FIM

:FIM
endlocal
