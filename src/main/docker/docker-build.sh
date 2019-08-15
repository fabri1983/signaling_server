#!/bin/sh

# decompress war file
mkdir target/docker-workdir
cd target/docker-workdir
jar -xf ../signaling.war
cd ../..

# create Docker image
docker image build \
	--build-arg DEPENDENCIES=docker-workdir \
	--build-arg JAVA_MAIN_CLASS=org.fabri1983.signaling.entrypoint.SignalingEntryPoint \
	-f target/Dockerfile -t fabri1983dockerid/signaling-server:dev ./target