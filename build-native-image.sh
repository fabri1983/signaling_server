#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 build-native-image.sh

# NOTE: you first need to build the signaling project and generate the JAR artifact targeting Java 8 or 11. Update pom.xml accordingly.

if [ -z "$GRAALVM_HOME" ] ; then
  echo "Please set GRAALVM_HOME to point to your graalvm installation"
  exit
fi

# build spring-graalvm-native project
echo === Building spring-graalvm-native
cd target/spring-graalvm-native/spring-graalvm-native-feature
mvn clean package
cd ../../..

export JAR="signaling.jar"

# decompress jar file to get a classpath with jars and classes
echo === Decompressing $JAR file to build a classpath with jars and classes
rm -rf target/graal-build 2> /dev/null
mkdir target/graal-build
cd target/graal-build
jar -xf ../$JAR
cp -R META-INF BOOT-INF/classes
# remove native-image folder because it is later read from the JAR file
rm -rf BOOT-INF/classes/META-INF/native-image 2> /dev/null
rm -rf META-INF/native-image 2> /dev/null

# build classpath with all jars and classes
cd WEB-INF/classes
export LIBPATH_1=`find ../lib | tr '\n' ':'`
export CP=.:$LIBPATH_1

# go back to graal-build folder
cd ../..

# spring-graal-native-feature jar being on the classpath is what triggers the Spring Graal auto configuration.
export CP=$CP:../spring-graalvm-native/spring-graalvm-native-feature/target/spring-graalvm-native-feature-0.7.0.BUILD-SNAPSHOT.jar

# compile with graal native-image
echo === Compiling with graal native-image
$GRAALVM_HOME/bin/native-image \
  --no-server \
  -cp $CP \
  -jar ../$JAR \
  -H:Class=org.fabri1983.signaling.SignalingEntryPoint
$GRAALVM_HOME/bin/native-image --server-shutdown

if [[ $? -eq 0 ]] ; then
	echo === Native image located at target/graal-build/
else
	echo === Failed!
fi

# let's go back to project base dir
cd ../..
