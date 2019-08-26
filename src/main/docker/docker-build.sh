#!/bin/sh
# If you need to change permissions for execution then do: sudo chmod 775 docker-build.sh

if [[ $# -ne 2 ]] ; then
  echo "No arguments supplied. You need to specify war final name (without .war) and tag name"
  exit 1
fi

echo -----------------------------
echo Decompressing $1.war
echo -----------------------------

# decompress war file
rm -rf target/docker-workdir 2> /dev/null
mkdir target/docker-workdir
cd target/docker-workdir
jar -xf ../$1.war
cd ../..

echo -----------------------------
echo Building Docker image
echo -----------------------------

# create Docker image
docker image build \
	--build-arg DEPENDENCIES=docker-workdir \
	--build-arg JAVA_MAIN_CLASS=org.fabri1983.signaling.entrypoint.SignalingEntryPoint \
	-f target/Dockerfile -t fabri1983dockerid/$1:$2 ./target

if [[ $? -eq 0 ]] ; then
	echo -----------------------------
	echo Finished!
	echo -----------------------------
else
	echo -----------------------------
	echo Failed!
	echo -----------------------------
fi