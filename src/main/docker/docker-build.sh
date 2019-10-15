#!/bin/sh
# If you need to change permissions for execution then do: sudo chmod 775 docker-build.sh

if [[ $# -ne 2 ]] ; then
  echo "No arguments supplied. You need to specify war final name (without .war) and tag name"
  exit 1
fi

echo -----------------------------
echo Decompressing $1.war
echo -----------------------------

# reset working directory
rm -rf target/docker-workdir 2> /dev/null
mkdir target/docker-workdir

# copy fabric8's run-java.sh
cp target/run-java.sh target/docker-workdir/

# decompress war file
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
	exit 0
else
	echo -----------------------------
	echo There was a problem!
	echo -----------------------------
	exit 1
fi
