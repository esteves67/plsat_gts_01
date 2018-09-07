@echo off
REM -----------------------------------------------------
REM  Valid Options:
REM     -db.sql.port:<port>    MySQL port
REM -----------------------------------------------------
REM ---
if "%GTS_HOME%" == "" echo Missing GTS_HOME environment variable
call "%GTS_HOME%\bin\common.bat"

REM ---
set JAR=%GTS_HOME%/build/lib/gtsadmin.jar
set ARGS=-conf:"%GTS_CONF%" -log.file.enable:false %1 %2 %3
if NOT "%GTS_DEBUG%"=="1" goto noEcho
    echo %CMD_JAVA% -jar %JAR% %ARGS%
:noEcho
%CMD_JAVA% -jar %JAR% %ARGS%

REM ---
