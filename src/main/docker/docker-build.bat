@ECHO OFF

IF "%1"=="" (
  GOTO NO_ARGS
)
IF "%2"=="" (
  GOTO NO_ARGS
)
IF NOT "%3"=="" (
  GOTO WRONG_ARGS
)

GOTO ACTION

:NO_ARGS
ECHO No arguments supplied. You need to specify war final name (without .war) and tag name
GOTO DONE

:WRONG_ARGS
ECHO Wrong number of arguments. You need to specify war final name (without .war) and tag name
GOTO DONE

:ACTION

ECHO -----------------------------
ECHO Decompressing %1.war
ECHO -----------------------------
:: NOTE: when using Spring Boot uber WAR we always need to decompress the WAR file since 
:: it comes with provided jars needed to start Tomcat or the selected Servlet engine.

:: reset working directory
RMDIR /Q /S target\docker-workdir > NUL 2>&1
MKDIR target\docker-workdir

:: copy fabric8's run-java.sh
COPY /Y target\run-java.sh target\docker-workdir

:: decompress war file
CD target\docker-workdir
jar -xf ..\%1.war
CD ..\..

ECHO -----------------------------
ECHO Building Docker image
ECHO -----------------------------

:: create Docker image
docker image build ^
	--build-arg DEPENDENCIES=docker-workdir ^
	--build-arg JAVA_MAIN_CLASS=org.fabri1983.signaling.entrypoint.SignalingEntryPoint ^
	-f target/Dockerfile -t fabri1983dockerid/%1:%2 ./target

if %ERRORLEVEL% == 0 (
	ECHO -----------------------------
	ECHO Finished!
	ECHO ----------------------------- 
) ELSE (
	ECHO -----------------------------
	ECHO Failed!
	ECHO ----------------------------- 
)

:DONE
EXIT /b