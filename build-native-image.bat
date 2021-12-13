@echo off
setlocal enableextensions enabledelayedexpansion

:: NOTE: you first need to build the signaling project and generate the JAR artifact targeting Java 11 or higher. Update pom.xml accordingly.
 
if "%GRAALVM_HOME%"=="" (
	echo Variable GRAALVM_HOME is NOT defined
	exit /b
)

:: build spring-native project
echo === Building spring-native
cd target\spring-native\spring-native
call mvn clean package
cd ..\..\..

set JAR=signaling.jar

:: decompress jar file to get a classpath with jars and classes
echo === Decompressing %JAR% file to build a classpath with jars and classes
rmdir /Q /S target\graal-build > NUL 2>&1
mkdir target\graal-build
cd target\graal-build
jar -xf ..\%JAR%
xcopy /E /Q /Y /S META-INF\* BOOT-INF\classes\META-INF\
:: remove native-image folder because it is later read from the JAR file
rmdir /Q /S BOOT-INF\classes\META-INF\native-image > NUL 2>&1
rmdir /Q /S META-INF\native-image > NUL 2>&1

:: build classpath with all jars and classes
cd BOOT-INF\classes
set LIBPATH_1=
for /r "..\lib" %%i in (*.jar) do set LIBPATH_1=!LIBPATH_1!%%i;
set CP=%CD%;%LIBPATH_1%

:: go back to graal-build folder
cd ..\..

:: spring-graal-native jar being on the classpath is what triggers the Spring Graal auto configuration.
:: we need to list only the exact jar since there is another one in test-classes
del /F /Q spring_graalvm_jars.txt > NUL 2>&1
dir /S /B ..\spring-native\spring-native\target\spring-graalvm-native-0.11.1-SNAPSHOT.jar > spring_graalvm_jars.txt
set FEATURES_JAR=
for /f %%i in (spring_graalvm_jars.txt) do set FEATURES_JAR=%%i;
set CP=%CP%;%FEATURES_JAR%

:: compile with graal native-image
echo === Compiling with graal native-image
call %GRAALVM_HOME%\bin\native-image ^
  -cp %CP% ^
  -jar ..\%JAR% ^
  -H:Class=org.fabri1983.signaling.SignalingEntryPoint
::$GRAALVM_HOME\bin\native-image --server-shutdown

if %ERRORLEVEL% == 0 (
	echo === Native image located at target\graal-build\
) else (
	echo === Failed!
)

:: let's go back to project base dir
cd ..\..

exit /b
