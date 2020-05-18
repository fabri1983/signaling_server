# Signaling Server with Spring Boot Websockets and Docker

[![Build Status](https://travis-ci.org/fabri1983/signaling_server.svg?branch=master)](https://travis-ci.org/fabri1983/signaling_server?branch=master)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Coverage Status](https://coveralls.io/repos/github/fabri1983/signaling_server/badge.svg?branch=master)](https://coveralls.io/github/fabri1983/signaling_server?branch=master)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Code Climate](https://codeclimate.com/github/fabri1983/signaling_server/badges/gpa.svg)](https://codeclimate.com/github/fabri1983/signaling_server)


This project uses [NextRTC Signaling Server](https://github.com/mslosarz/nextrtc-signaling-server) project (which is no longer maintained).
I added custom signals, room max participants limit, and other constraints, to provide a complete video call solution between two clients.
It is cluster aware by using a distributed event bus backed by *Hazelcast* with node auto discovery.


- Uses Maven 3.6.x. You can use `mvnw` if you don't have Maven installed in your host.
- Uses Spring Boot 2.3.0.RELEASE.
- After Spring Boot repackages the final *JAR* file, a Docker image can be built using the profile `docker`. So you need to get Docker installed and running.
- Runs on **Java 8** and **Java 11** (default profile is `java11`).
- Native image generation using GraalVM: It is enabled with profile `graal`. Currently struggling with *Spring Graal Native* plugin to correctly create a native image.


## Create self signed certificate (no chain ca, no SAN -Subject Alternative Names-)
*(Skip this step if you want to use current self signed certificate or already have your own certificate in your keystore.*
*Don't forget to edit application.properties file accordingly)*

Generate a self-signed certificate and import it into custom keystore:
```bash
cd src/main/resources
rm -f local-keystore.jks
keytool -genkey -alias serverca -validity 1095 -keyalg RSA -keysize 2048 -dname "cn=Server" -ext bc:c -keystore local-keystore.jks -keypass servercapass -storepass servercapass
```
Edit `application.properties` accordingly if you have made changes on any of above information.  

*Optional*: Export certificate file in X.509 format:
```bash
keytool -export -rfc -alias serverca -keystore local-keystore.jks -storepass servercapass -file signaling-self.crt
```
The rfc keyword specifies base64-encoded output.


## Create self signed certificate (chain ca, with SAN -Subject Alternative Names-)
*(Skip this step if you want to use current self signed certificate or already have your own certificate in your keystore.*
*Don't forget to edit application.properties file accordingly)*

Script `chain_certificate.<sh|bat>` generates a chain certificate signed by a root certificate. Provides SAN info to validate local domains as:
`localhost, 127.0.0.1, 192.168.99.100, 172.17.0.2, 172.17.0.3`.  
Last three ips belongs to Docker host ips in Windows and Linux.

**Linux:**
```bash
cd src/main/resources/scripts
chain_certificate.sh
cd ..
mv -f scripts/local-keystore.jks local-keystore.jks
```

**Windows:**
```bash  
cd src\main\resources\scripts
chain_certificate.bat
cd ..
move /Y scripts\local-keystore.jks local-keystore.jks
	Option /Y forces overwrite exisitng file
```

Edit `application.properties` accordingly if you have made changes on `chain-certificate.<sh|bat>` script.


## Create key pair for signing JWTs (Json Web Token)
Current videochat example uses insecure endpoint so any authorization is skipped.  
**If you want to use the secure endpoint then you will need to generate a key pair and a valid JWT.**  

We are going to generate public and private key pair using RSA algorithm to later use them to sign JWTs.  
Enter into `profiles` folder.
```bash
cd src/main/resources/profiles
```
Ouput private key in PEM or base64 format:
```bash
openssl genrsa -out jwt_local_private.key 2048
```
Convert private Key to PKCS#8 format (so Java can read it):
```bash
openssl pkcs8 -topk8 -inform PEM -outform DER -in jwt_local_private.key -out jwt_local_private.der -nocrypt
```
Output public key portion in PEM or base64 format:
```bash
openssl rsa -in jwt_local_private.key -pubout -out jwt_local_public.key
rm -f jwt_local_private.key
```
Output public key portion in DER format (so Java can read it):
```bash
openssl rsa -in jwt_local_private.key -pubout -outform DER -out jwt_local_public.der
```

### Generate valid token for secure endpoint usage
- First you need to get your user id. You can obtain it directly from the `User Id` input textbox form the videochat example page.
- If you have opened a second window/tab for videochat then keep that new `User Id` too.
- Visit [jwt.io](https://jwt.io) Debugger section, and generate a RSA256 token:
  - you will need to copy content from files:
    - `src/main/resources/profiles/jwt_local_private.key`
    - `src/main/resources/profiles/jwt_local_public.key`
  - use next image as a reference:
    ![jwt token generation](/jwt_token_generation.jpg?raw=true "jwt token generation")
- Use that token to populate HTTP Header `vctoken` (on browsers you will need a plugin).
- Use the first *User Id* value (*fromUid*) to populate HTTP Header `vcuser` (on browsers you will need a plugin). 


## Maven Profiles
**Profiles**
- `local` (active by default)  
Sets JWT's signer and verifier private and public keys.

- `java11` (active by default)  
Sets compiler target to **Java 11** and also uses *Dockerfile.java11* file for image generation.

- `java8`  
Sets compiler target to **Java 8** and also uses *Dockerfile.java8* file for image generation.

- `eventbus-local` (active by default)  
Disable a Spring profile repsonsibly to allow communication between nodes.

- `eventbus-hazelcast`  
Enables a Spring profile which allows the use of a distributed eventbus between exisiting nodes.

- `docker`  
Fires a docker image creation after package is created. It currenly disables native image generation option.

- `graal`  
Native image generation using Graal Native Image. It currently disables docker creation option.
    
Example:
```sh
mvn clean package -P local,eventbus-hazelcast,java11
```


## Spring Boot Standalone JAR:

#### Eclipse IDE
- Import as Maven project.
- Active profiles: using `ALT+SHIFT+P` select *local*, *eventbus-local*, *java11* profiles.
- Edit *application.properties* accordingly. Be aware *server.port* value is *8443*.
- Build project: `CTRL+B`.
- You can the app if using *Eclipse Spring Suite Tools*: just run `SignlaingEntryPoint` as Spring Boot App.

#### Maven pom and Spring Bean Configuration setup
- Edit *application.properties* accordingly. Be aware *server.port* value is *8443*.
- Run:
```sh
mvn clean package
java -jar target/signaling.jar
```
	
#### Access
- From your client app access it via:
	- [wss://127.0.0.1:8443/signaling/v1/s-insecure](wss://127.0.0.1:8443/signaling/v1/s-insecure)  
- Or the secured endpoint which after HTTP Upgrade to Websocket it expects and validates headers *vcuser* and *vctoken*:
	- [wss://127.0.0.1:8443/signaling/v1/s](wss://127.0.0.1:8443/signaling/v1/s)  
See **NextRTC Video Chat exmaple** section.


## Curious multiple log of same message
The next message repeats in the logs 3 times:  
```sh
... INFO  [main]  NextRTCEndpoint: Setted server: org.nextrtc.signalingserver.domain.Server@61c58320 to org.fabri1983.signaling.configuration.SignalingConfiguration$2@10e4ee33
... INFO  [main]  NextRTCEndpoint: Setted server: org.nextrtc.signalingserver.domain.Server@61c58320 to org.fabri1983.signaling.configuration.SignalingConfiguration$2@10e4ee33
... INFO  [main]  NextRTCEndpoint: Setted server: org.nextrtc.signalingserver.domain.Server@61c58320 to org.fabri1983.signaling.configuration.SignalingConfiguration$2@10e4ee33
```
This is due to the nature of `NextRTCEndpoint` class which has a mix of `Singleton` pattern, `@Component`, and has `@Inject` in method `setServer()`. 
Nevertheless, as the log lines show, the `Server` bean is created only once. And so it does for the `NextRTCEndpoint` bean defined in the 
`SignalingConfiguration` class.


## NextRTC Video Chat usage

- Enter [https://127.0.0.1:8443/signaling/videochat.html](https://127.0.0.1:8443/signaling/videochat.html) in any browser.
The use of **https** is important because default http handler isn't configured.

- Accept untrusted certificate.

- You can use query param *forceTurn=true* in order to force relay ICE Transport Policy and so test your TURN server:
[https://127.0.0.1:8443/signaling/videochat.html?forceTurn=true](https://127.0.0.1:8443/signaling/videochat.html?forceTurn=true)
  
_Sometimes websocket (js side) is throwing an exception and can't connect via websocket o signiling server, then try to change localhost to 127.0.0.1_
  
This is a working test of the Signaling Server and the videochat client using a Chrome tab on my laptop and an Opera tab on my mobile phone. 
Server exposed with [ngrok](https://ngrok.com/).

![videochat with local signaling](videochat_example_ngrok.jpg?raw=true "videochat with local signaling")


## jdeps on a Spring Boot Fat JAR file

Use `jdeps` to know which java modules the final application needs to run. Note that we are using `--multi-release=11`.

- *NOTE*: *this guide is only valid for Spring Boot fat JAR due to internal structure.*

- Windows:
```bash
mkdir target\docker-workdir
cd target\docker-workdir && jar -xf ..\signaling.jar && cd ..\..
jdeps --add-modules=ALL-MODULE-PATH --ignore-missing-deps --multi-release=11 --print-module-deps ^
  -cp target\docker-workdir\BOOT-INF\lib\* target\docker-workdir\BOOT-INF\classes
```

- Linux:
```bash
mkdir target\docker-workdir
cd target\docker-workdir && jar -xf ..\signaling.jar && cd ..\..
jdeps --add-modules=ALL-MODULE-PATH --ignore-missing-deps --multi-release=11 --print-module-deps \
  -cp target/docker-workdir/BOOT-INF/lib/* target/docker-workdir/BOOT-INF/classes
```

- Example Output:
```bash
java.base,java.compiler,java.desktop,java.instrument,java.management.rmi,java.naming,java.prefs,java.scripting,java.security.jgss,java.sql,jdk.httpserver,jdk.unsupported
```


## Run with Docker and test Distributed Event Bus with Hazelcast

- *NOTE*: this guide is only valid for Spring Boot Fat JAR due to internal structure.

- First pack the Signaling Server in a Fat JAR artifact using Spring Boot maven plugin:
```bash
mvn clean package -P local,eventbus-hazelcast,java11,docker
```

- **Create a multi layer Docker image for Spring Boot app**:
In order to take advantage of less frequency changes the [Dockerfile.java8](src/main/docker/Dockerfile.java8) file and 
[Dockerfile.java11](src/main/docker/Dockerfile.java11) file both define a multi layer image, so next time image build is 
fired it only updates application code.  
Script **docker-build.<bat|sh>** is moved to `target` folder during maven life cycle.  
It decompress the JAR file and creates the multi layer Docker image.  
Keep an eye on the context size sent to Docker's context:
```bash
Sending build context to Docker daemon  36.12MB   (this is the size with all libs)
```  
Once the image build finishes use next command to check layers size:
```bash
docker history fabri1983dockerid/signaling:dev
```

A Java process is a regular Windows/Linux process. How much actual physical memory this process is consuming?
Or in other words: what is the **Resident Set Size (RSS)** value for running a Java process?

Theoretically, in the case of a Java application, a required RSS size can be calculated by:
```
RSS = Heap size + MetaSpace + OffHeap size
```
where OffHeap consists of thread stacks, direct buffers, mapped files (libraries and jars) and JVM code itself.

See this article to beter understand how Java memory is used in Docker:
http://trustmeiamadeveloper.com/2016/03/18/where-is-my-memory-java/

- **Run 2 instances of the image**:
```bash
docker container run -i -m 400m -p 8481:8443 --name signaling-1 fabri1983dockerid/signaling:dev
docker container run -i -m 400m -p 8482:8443 --name signaling-2 fabri1983dockerid/signaling:dev
(replace -i by -d if you want to detach the process and let it run on background)

Then manage it with:
docker container stop|start <container-name>

Connect to its bash console:
docker container exec -it <container-name> /bin/ash
```
Or you can use the [docker-compose-local.yml](src/main/docker/docker-compose-local.yml):
```bash
docker-compose -f target/docker-compose-local.yml up

Then manage it with:
docker-compose -f target/docker-compose-local.yml stop|start
```

- Test the Distributed Event Bus with Hazelcast:
  - If you are using docker in **Windows** with **Docker Tool Box** then visit:
    - [videochat-1](https://192.168.99.100:8481/signaling/videochat.html)
    - [videochat-2](https://192.168.99.100:8482/signaling/videochat.html)
  - If on **Linux**:
    - [videochat-1](https://172.17.0.2:8481/signaling/videochat.html)
    - [videochat-2](https://172.17.0.3:8482/signaling/videochat.html)
    - in case you need running Docker ip:
    ```bash
    docker inspect -f "{{ .NetworkSettings.IPAddress }}" <containerNameOrId>
    ```


## Native Image generation with GraalVM using Maven native-image plugin
**WIP. Currently taking infinte amount of time to build native-image. Investigating.**  
- First set `GRAALVM_HOME` environment variable to point *GraalVM Java 8* or *Java 11* (depending on what graalvm installation you are targeting).
- Second set `JAVA_HOME` environment variable to point *GraalVM*. Update your `PATH` as well.
- Then build the signaling project and generate the JAR artifact for *java8* or *java11* (depending on what graalvm installation you are targeting).
  - Update `pom.xml` modifying Spring Boot version to 2.3.0.RC1 (only if you current Spring Boot version doesn't match).
  - Build package:
  ```bash
  mvn clean package -P graal,local,eventbus-hazelcast,java8
  ```
  This will generate native image (**you will need 3.8 GB of free memory!**)


## Native Image generation with GraalVM using custom scripts
**WIP. Currently taking infinte amount of time to build native-image. Investigating.**  
- First set `GRAALMV_HOME` environment variable to point *GraalVM Java 8* or *Java 11* (depending on what graalvm installation you are targeting).
- Second set `JAVA_HOME` environment variable to point *GraalVM*. Update your `PATH` as well.
- Then build the signaling project and generate the JAR artifact for *java8* or *java11* (depending on what graalvm installation you are targeting).
  - Update `pom.xml` modifying Spring Boot version to 2.3.0.RC1 (only if you current Spring Boot version doesn't match).
  - Build package:
  ```bash
  mvn clean package -P graal,local,eventbus-hazelcast,java8 -Dskip.native.build=true
  ```
- Locate at project root dir and download the [Spring-Graal-Native-Image](https://github.com/spring-projects-experimental/spring-graal-native.git) project:  
(Next scripts will clone it under target folder)
```bash
Windows:
  clone-spring-graal-native.bat
Linux
  clone-spring-graal-native.sh
```
- Generate native image from JAR artifact (**you will need 3.8 GB of free memory!**):  
Signaling JAR file contains `META-INF/native-image/org.fabri1983.signaling/native-image.properties` with all the options/flags.
```bash
Windows:
  build-native-image.bat
Linux
  build-native-image.sh
```


## OWASP Dependency Checker
Run next command to check if any dependency has a security risk according the Maven plugin *dependency-checker* from **OWASP**:  
```sh
mvn verify -P local,eventbus-hazelcast,java11,securitycheck -Dskip.docker.build=true
```


## TODO
- Currently hazelcast configuration uses multicast for service discovery. Add different solutions: 
https://hazelcast.com/blog/hazelcast-auto-discovery-with-eureka/
