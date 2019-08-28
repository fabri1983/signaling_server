#!/bin/bash
# If you need to change permissions for execution then do: sudo chmod 775 build-native-image.sh

# NOTE: you first need to build the project and generate the WAR artifact targeting Java 8. Update pom.xml.

if [ -z "$GRAALVM_HOME" ] ; then
  echo "Please set GRAALVM_HOME to point to your graalvm installation"
  exit
fi

# download spring-boot-graal-feature and build it as jar
echo :::::::: Download spring-boot-graal-feature
rm -rf target/spring-boot-graal-feature 2> /dev/null
git clone --single-branch --branch graal_19_2_0_dev https://github.com/aclement/spring-boot-graal-feature.git target/spring-boot-graal-feature

echo :::::::: Building spring-boot-graal-feature
cd target/spring-boot-graal-feature
mvn clean package
cd ../..

export WAR="signaling.war"
export IMAGE_NAME="signaling"

# decompress war file to get a classpath with jars and classes
echo :::::::: Decompressing $WAR file to build a classpath with jars and classes
rm -rf target/graal-build 2> /dev/null
mkdir target/graal-build
cd target/graal-build
jar -xf ../$WAR

# build classpath with all jars and classes
cd WEB-INF/classes
export LIBPATH_1=`find ../lib | tr '\n' ':'`
export LIBPATH_2=`find ../lib-provided | tr '\n' ':'`
export CP=.:$LIBPATH_1:$LIBPATH_2

# go back to graal-build folder
cd ../..

# spring-boot-graal-feature being on the classpath is what triggers it
export CP=$CP:../spring-boot-graal-feature/target/spring-boot-graal-feature-0.5.0.BUILD-SNAPSHOT.jar

# compile with graal native-image
echo :::::::: Compiling with graal native-image
$GRAALVM_HOME/bin/native-image \
  -J-Xmx4000m \
  -H:+ReportExceptionStackTraces \
  -H:+TraceClassInitialization \
  -Dio.netty.noUnsafe=true \
  -H:Name=$IMAGE_NAME \
  -H:IncludeResources=".*.properties|.*.jks|.*.key|.*.xml|.*.js|.*.html|.*.jsp" \
  --no-fallback \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  -cp $CP -jar ../$WAR

if [[ $? -eq 0 ]] ; then
	echo :::::::: Native image located at target/graal-build/
else
	echo :::::::: Failed!
fi

# let's go back to project base dir
cd ../..
