@ECHO OFF

IF "%1"=="" (
  GOTO NO_ARGS
)
IF "%2"=="" (
  GOTO NO_ARGS
)
IF "%3"=="" (
  GOTO NO_ARGS
)
IF "%4"=="" (
  GOTO NO_ARGS
)
IF NOT "%5"=="" (
  GOTO WRONG_ARGS
)

GOTO ACTION

:NO_ARGS
ECHO No arguments supplied. You need to specify artifact final name (without extension), extension (war or jar), tag name, and java class
GOTO FAILED

:WRONG_ARGS
ECHO Wrong number of arguments. You need to specify artifact final name (without extension), extension (war or jar), tag name, and java class
GOTO FAILED

:ACTION

ECHO -----------------------------
ECHO Decompressing %1.%2
ECHO -----------------------------
:: NOTE: when using Spring Boot fat WAR we always need to decompress the WAR file since 
:: it comes with provided jars needed to start Tomcat (or the selected Servlet engine).

:: reset working directory
RMDIR /Q /S target\docker-workdir > NUL 2>&1
MKDIR target\docker-workdir

:: copy fabric8's run-java.sh
COPY /Y target\run-java.sh target\docker-workdir

:: decompress artifact file
CD target\docker-workdir
jar -xf ..\%1.%2
CD ..\..

ECHO -----------------------------
ECHO Building Docker image
ECHO -----------------------------

SET tagName=fabri1983dockerid/%1:%3

:: create Docker image
docker image build ^
	--build-arg DEPENDENCIES=docker-workdir ^
	--build-arg JAVA_MAIN_CLASS=%4 ^
	-f target/Dockerfile -t %tagName% ./target

if %ERRORLEVEL% == 0 (
	ECHO ----------------------------------------------------------
	ECHO Finished! Docker Image tagged: %tagName%
	ECHO ----------------------------------------------------------
	GOTO SUCCESS
) ELSE (
	ECHO -----------------------------
	ECHO There was a problem!
	ECHO -----------------------------
	GOTO FAILED
)

:FAILED
EXIT /b 1

:SUCCESS
EXIT /b 0