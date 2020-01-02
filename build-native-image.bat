@echo off
setlocal enableextensions enabledelayedexpansion

:: NOTE: you first need to build the signaling project and generate the WAR artifact targeting Java 8. Update pom.xml accordingly.
 
if "%GRAALVM_HOME%"=="" (
	echo Variable GRAALVM_HOME is NOT defined
	exit /b
)

:: build spring-graal-native project
echo :::::::: Building spring-graal-native
cd target\spring-graal-native\spring-graal-native-feature
call mvn clean package
cd ..\..\..

set WAR=signaling.war

:: decompress war file to get a classpath with jars and classes
echo :::::::: Decompressing %WAR% file to build a classpath with jars and classes
rmdir /Q /S target\graal-build > NUL 2>&1
mkdir target\graal-build
cd target\graal-build
jar -xf ..\%WAR%
xcopy /E /Q /Y /S META-INF\* WEB-INF\classes\META-INF\
:: remove native-image folder because it is later read from the WAR file
rmdir /Q /S WEB-INF\classes\META-INF\native-image > NUL 2>&1
rmdir /Q /S META-INF\native-image > NUL 2>&1

:: build classpath with all jars and classes
cd WEB-INF\classes
set LIBPATH_1=
set LIBPATH_2=
for /r "..\lib" %%i in (*.jar) do set LIBPATH_1=!LIBPATH_1!%%i;
for /r "..\lib-provided" %%i in (*.jar) do set LIBPATH_2=!LIBPATH_2!%%i;
set CP=%CD%;%LIBPATH_1%;%LIBPATH_2%

:: go back to graal-build folder
cd ..\..

:: spring-graal-native-feature jar being on the classpath is what triggers the Spring Graal auto configuration.
:: we need to list only the exact jar since there is another one in test-classes
del /F /Q features_jar.txt > NUL 2>&1
dir /S /B ..\spring-graal-native\spring-graal-native-feature\target\spring-graal-native-feature-0.6.0.BUILD-SNAPSHOT.jar > features_jar.txt
set FEATURES_JAR=
for /f %%i in (features_jar.txt) do set FEATURES_JAR=%%i;
set CP=%CP%;%FEATURES_JAR%

:: compile with graal native-image
echo :::::::: Compiling with graal native-image
call %GRAALVM_HOME%\bin\native-image ^
  --verbose ^
  -cp %CP% -jar ..\%WAR%
::  -cp %CP% org.fabri1983.signaling.SignalingEntryPoint

if %ERRORLEVEL% == 0 (
	echo :::::::: Native image located at target\graal-build\
) else (
	echo :::::::: Failed!
)

:: let's go back to project base dir
cd ..\..

exit /b
