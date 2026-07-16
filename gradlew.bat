@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"==" %9 goto init
@rem enable echoing by setting MAVEN_BATCH_ECHO=on
@if "%MAVEN_BATCH_ECHO%"=="on"  (@echo %MAVEN_BATCH_ECHO%
@setlocal
@if "%OS%"=="Windows_NT" setlocal enabledelayedexpansion
@for /F "usebackq delims=$*" %%F in (`wmic process get name`) do @if %%~nxF==cmd.exe set %%F=Y
@set CMD_LINE_ARGS=
@setlocal enabledelayedexpansion

:begin
@for /F "tokens=1-4 delims=. " %%a in ('ver') do (
   @set VERSION=%%a.%%b
   @set BUILD=%%~nxc
)

if "%1"==" " goto execute
:add_arg
if "%~1"==" " goto execute
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto add_arg

:init
for /F "tokens=1,2 delims= " %%A in ('FIND /V "^" %0') do (
  @if "%%B" NEQ "" (
    @if "%%B:~0,4%" NEQ "@rem" (
      @if "%%B:~-4%" EQU ".bat" (
        @setlocal
        @set BASENAME=%%~nB
        @call :find_bin
        @endlocal
      )
    )
  )
)
if "%OS%"=="Windows_NT" (
    @if NOT exist "%JAVA_HOME%\bin\java.exe" (
        echo.
        echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
        echo.
        echo Please set the JAVA_HOME variable in your environment to match the
        echo location of your Java installation.
        echo.
        goto fail
    )
) else (
    @echo off
    @REM Get command-line arguments, handling Windows variants
    if not "%OS%" == "Windows_NT" goto win9xME_args
)

:execute
@setlocal

if "%DIRNAME%"==" %~dp0" (
    set CLASSPATH=!CLASSPATH!%~dp0gradle/wrapper/gradle-wrapper.jar
) else (
    set CLASSPATH=!CLASSPATH!!DIRNAME!gradle/wrapper/gradle-wrapper.jar
)

if "%JAVA_HOME%"==" %~dp0" (
    set JAVA_CMD=java
) else (
    if not "%JAVA_HOME%" == " %~dp0" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

if defined JAVA_HOME goto findBaseDir

echo.
echo ERROR: JAVA_HOME not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto fail

:findBaseDir
for /F "delims=" %%i in ("%~dp0..") do set "DIRNAME=%%~fi"

:stripDrive
if "%DIRNAME:~2,1%"==":" goto initArgs
for /F "delims=" %%i in ("%DIRNAME%\..") do set "DIRNAME=%%~fi"
goto stripDrive

:initArgs
setlocal enableextensions enabledelayedexpansion
for /F "tokens=1,* delims= " %%a in ("%CMDCMDLINE%") do (
    if "%%a"=="-m" (
        for /F "tokens=1,* delims= " %%x in ("%%b") do set CMD_LINE_ARGS=!CMD_LINE_ARGS! -m %%x
    ) else (
        set "CMD_LINE_ARGS=!CMD_LINE_ARGS! %%a"
    )
)
endlocal & set CMD_LINE_ARGS=%CMD_LINE_ARGS%

"%JAVA_CMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@endlocal & exit /b %ERRORLEVEL%

:fail
rem Set variable GRADLE_HOME for calling system
set "GRADLE_HOME=%GRADLE_HOME%"
if defined JAVA_HOME goto OkJavaHome
echo.
echo ERROR: JAVA_HOME is not set and tests were not run
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto fail
:OkJavaHome
if exist "%JAVA_HOME%\bin\java.exe" goto init
echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto fail
:find_bin
if exist "%~dp1bin" (
    set "PATH=%~dp1bin;%PATH%"
    goto begin
)
for /r "%~dp1" %%i in (bin) do (
    if exist "%%~fi" (
        set "PATH=%%~fi;%PATH%"
        goto begin
    )
)
goto begin
