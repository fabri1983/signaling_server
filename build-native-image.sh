#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 build-native-image.sh

# NOTE: you first need to build the signaling project and generate the WAR artifact targeting Java 8. Update pom.xml accordingly.

if [ -z "$GRAALVM_HOME" ] ; then
  echo "Please set GRAALVM_HOME to point to your graalvm installation"
  exit
fi

# build spring-graal-native project
echo :::::::: Building spring-graal-native
cd target/spring-graal-native/spring-graal-native-feature
mvn clean package
cd ../../..

export WAR="signaling.war"

# decompress war file to get a classpath with jars and classes
echo :::::::: Decompressing $WAR file to build a classpath with jars and classes
rm -rf target/graal-build 2> /dev/null
mkdir target/graal-build
cd target/graal-build
jar -xf ../$WAR
cp -R META-INF WEB-INF/classes

# build classpath with all jars and classes
cd WEB-INF/classes
export LIBPATH_1=`find ../lib | tr '\n' ':'`
export LIBPATH_2=`find ../lib-provided | tr '\n' ':'`
export CP=.:$LIBPATH_1:$LIBPATH_2

# go back to graal-build folder
cd ../..

:: spring-graal-native-feature jar being on the classpath is what triggers the Spring Graal auto configuration.
export CP=$CP:../spring-graal-native/spring-graal-native-feature/target/spring-graal-native-feature-0.6.0.BUILD-SNAPSHOT.jar

# compile with graal native-image
echo :::::::: Compiling with graal native-image
$GRAALVM_HOME/bin/native-image --no-server \
  --verbose \
  -cp $CP -jar ../$WAR
#  -cp $CP org.fabri1983.signaling.entrypoint.SignalingEntryPoint

$GRAALVM_HOME/bin/native-image --server-shutdown

if [[ $? -eq 0 ]] ; then
	echo :::::::: Native image located at target/graal-build/
else
	echo :::::::: Failed!
fi

# let's go back to project base dir
cd ../..
