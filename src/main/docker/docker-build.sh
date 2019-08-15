#!/bin/sh
# If you need to change permissions for execution then do: sudo chmod 775 docker-build.sh

if [[ $# -eq 0 ]] ; then
  echo "No arguments supplied. You need to specify war final name (without .war)"
  exit 1
fi
if [[ $# -gt 1 ]] ; then
  echo "Wrong number of arguments. You need to specify war final name (without .war)"
  exit 1
fi

echo -----------------------------
echo Decompressing $1.war
echo -----------------------------

# decompress war file
mkdir target/docker-workdir
cd target/docker-workdir
jar -xf ../$1.war
cd ../..

echo -----------------------------
echo Buidling Docker image
echo -----------------------------

# create Docker image
docker image build \
	--build-arg DEPENDENCIES=docker-workdir \
	--build-arg JAVA_MAIN_CLASS=org.fabri1983.signaling.entrypoint.SignalingEntryPoint \
	-f target/Dockerfile -t fabri1983dockerid/$1-server:dev ./target

echo ---------
echo Finished!
echo ---------