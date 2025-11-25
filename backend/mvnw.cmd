@echo off
@REM Maven Wrapper for Windows

@REM Enable command extensions and delayed variable expansion
setlocal enableextensions enabledelayedexpansion

@REM Set the project base directory (strip trailing backslash)
set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

@REM Set Maven Wrapper JAR path
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@REM Check if Maven Wrapper JAR exists
if not exist "%WRAPPER_JAR%" (
    echo ERROR: Maven Wrapper JAR not found at: %WRAPPER_JAR%
    echo Please run: mvn -N io.takari:maven:wrapper
    exit /b 1
)

@REM Find Java
if not "%JAVA_HOME%"=="" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_EXE=java
)

@REM Check Java exists
where "%JAVA_EXE%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found. Please set JAVA_HOME or add java to PATH
    exit /b 1
)

@REM Run Maven Wrapper with maven.multiModuleProjectDirectory set
"%JAVA_EXE%" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  -cp "%WRAPPER_JAR%" ^
  %WRAPPER_LAUNCHER% ^
  %*

@REM Preserve exit code
set EXIT_CODE=%ERRORLEVEL%

endlocal & exit /b %EXIT_CODE%
