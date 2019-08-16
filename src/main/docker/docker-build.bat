@ECHO OFF

IF "%1"=="" (
  GOTO NO_ARGS
)
IF NOT "%2"=="" (
  GOTO WRONG_ARGS
)

GOTO ACTION

:NO_ARGS
ECHO No arguments supplied. You need to specify war final name (without .war)
GOTO DONE

:WRONG_ARGS
ECHO Wrong number of arguments. You need to specify war final name (without .war)
GOTO DONE

:ACTION

ECHO -----------------------------
ECHO Decompressing %1.war
ECHO -----------------------------
:: NOTE: when using Spring Boot uber WAR we always need to decompress the WAR file since 
:: it comes with provided jars needed to start Tomcat or the selected Servlet engine.

:: decompress war file
mkdir target\docker-workdir
cd target\docker-workdir
jar -xf ..\%1.war
cd ..\..

ECHO -----------------------------
ECHO Building Docker image
ECHO -----------------------------

:: create Docker image
docker image build ^
	--build-arg DEPENDENCIES=docker-workdir ^
	--build-arg JAVA_MAIN_CLASS=org.fabri1983.signaling.entrypoint.SignalingEntryPoint ^
	-f target/Dockerfile -t fabri1983dockerid/%1-server:dev ./target

ECHO -----------------------------
ECHO Finished!
ECHO ----------------------------- 

:DONE
EXIT /b