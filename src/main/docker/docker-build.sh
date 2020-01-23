#!/bin/sh
# If you need to change permissions for execution then do: sudo chmod 775 docker-build.sh

if [[ $# -ne 4 ]] ; then
  echo "No arguments supplied. You need to specify artifact final name (without extension), extension (war or jar), tag name, and java class"
  exit 1
fi

echo -----------------------------
echo Decompressing $1.$2
echo -----------------------------
# NOTE: when using Spring Boot fat WAR we always need to decompress the WAR file since 
# it comes with provided jars needed to start Tomcat (or the selected Servlet engine).

# reset working directory
rm -rf target/docker-workdir 2> /dev/null
mkdir target/docker-workdir

# copy fabric8's run-java.sh
cp target/run-java.sh target/docker-workdir/

# decompress artifact file
cd target/docker-workdir
jar -xf ../$1.$2
cd ../..

echo -----------------------------
echo Building Docker image
echo -----------------------------

tagName=fabri1983dockerid/$1:$3

# create Docker image
docker image build \
	--build-arg DEPENDENCIES=docker-workdir \
	--build-arg JAVA_MAIN_CLASS=$4 \
	-f target/Dockerfile -t $tagName ./target

if [[ $? -eq 0 ]] ; then
	echo ----------------------------------------------------------
	echo Finished! Docker Image tagged: $tagName
	echo ----------------------------------------------------------
	exit 0
else
	echo -----------------------------
	echo There was a problem!
	echo -----------------------------
	exit 1
fi
