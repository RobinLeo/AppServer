@echo off
set BASE_DIR=%~dp0..

:LogsOK
if not "%JAVA_HOME%" == "" goto OkJHome
echo.
echo 请配置JAVA_HOME
echo.
goto error

:OkJHome
echo Using BASE_DIR:   %BASE_DIR%
echo Using JAVA_HOME:  %JAVA_HOME%
set JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8888,server=y,suspend=n -Xmx512M

"%JAVA_HOME%/bin/java" %JAVA_OPTS% -cp %BASE_DIR%/bin; -Djava.ext.dirs=%BASE_DIR%/lib com.robin.im.ServerStart %1 %2 %3 %4 %5
goto :ok

pause
:ok
