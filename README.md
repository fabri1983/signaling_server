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
- Uses Spring Boot 2.2.0.RELEASE.
- After Spring Boot repackages the final *WAR* file, a Docker image is built. So you need to get Docker installed and running. 
If not Docker installed then use `-Dskip.docker.build=true` to skip the docker build.
- Runs on **Java 11** (default profile is *java11*). If you want to use **Java 8** then you need to use maven profile *java8*. 
- Native image generation using GraalVM: currently struggling with *Spring Graal Native* plugin to correctly create a native image.


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
Sets compiler target to **Java 11** and also uses Dockerfile.java11 file for image generation.

- `java8`    
Sets compiler target to **Java 8** and also uses Dockerfile.java8 file for image generation.

**Additional profiles**  
- `eventbus-local` (active by default)  
- `eventbus-hazelcast`  
Removes/adds additional dependencies and disables/enables a Spring profile which allows the use of a distributed eventbus.
When using *eventbus-local* some dependencies are removed and the beans defined in `DistributedSignalingConfiguration` are not created.
When using *eventbus-hazelcast* the opposite occurs.  
  
Example:  
```sh
mvn clean package -P local,eventbus-hazelcast,java11
```


## Build WAR file to deploy in Tomcat (external or with Cargo plugin)

#### Maven pom and Spring Bean Configuration setup
- Edit *pom.xml*:  
	- add next property:  
		```xml
		<javax.websocket.api.version>1.1</javax.websocket.api.version>
		```  
	- add next dependencies:  
		```xml
		<!-- Java JSR 356 WebSocket -->
		<dependency>
			<groupId>javax.websocket</groupId>
			<artifactId>javax.websocket-api</artifactId>
			<version>${javax.websocket.api.version}</version>
			<scope>provided</scope> <!-- provided by Tomcat -->
		</dependency>
		<!-- Java JSR 340 Servlet 3.1 or higher -->
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>javax.servlet-api</artifactId>
			<scope>provided</scope> <!-- provided by Tomcat -->
		</dependency>
		```  
	- remove next dependencies (if exist):  
		```xml
		<groupId>org.springframework</groupId>
		<artifactId>spring-websocket</artifactId>
		```  
	- NextRTC dependency needs to exclude Spring Context dependency:  
		```xml
		<dependency>
			<groupId>org.nextrtc.signalingserver</groupId>
			<artifactId>nextrtc-signaling-server</artifactId>
			<version>${nextrtc.signaling.server.version}</version>
			<exclusions>
				<exclusion>
					<groupId>org.springframework</groupId>
					<artifactId>spring-context</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		```  
	- remove next build plugins:  
		```xml		
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-maven-plugin</artifactId>
		```  
- Then remnove next beans (if exist) in *SignalingConfiguration* class:
	- `ServerEndpointExporter serverEndpointExporter()`
- Edit *application.properties* accordingly. Be aware *server.port* value is *8443*.

#### Eclipse IDE
- Import as Maven project
- Configure Dynamic Web project: *Properties -> Project Facets*
- Set context root to **signaling**: 
	- *Properties -> Web Project Settings*
- Active profiles: using `ALT+SHIFT+P` select *local*, *eventbus-local*, *java11* profiles.
- Build project: `CTRL+B`

#### Deploy on external Tomcat installation
- Use next configuration on your *<CATALINA_BASE>/conf/server.xml*:
	```xml
	<Connector port="8443" protocol="org.apache.coyote.http11.Http11Nio2Protocol" 
		SSLEnabled="true" clientAuth="false" keyAlias="serverca" keystoreFile="conf/local-keystore.jks" 
		keystorePass="<keystore-pass>" scheme="https" secure="true" sslProtocol="TLS" />
	```
	- You will need to copy local-keystore.jsk into *<CATALINA_BASE>/conf/* folder.
	- In case you already have a keystore in Tomcat *<CATALINA_BASE>/conf/* folder then you will need to import the signaling certificate into it.
- Then create Server in Eclipse and choose existing Tomcat installation.
- Deploy from *Eclipse's Server tab*.

#### Deploy using Cargo plugin with Tomcat
Add next plugin on *build* section:  
```xml
<plugin>
  <groupId>org.codehaus.cargo</groupId>
  <artifactId>cargo-maven2-plugin</artifactId>
  <version>1.7.5</version>
  <configuration>
    <container>
      <containerId>tomcat8x</containerId>
      <containerUrl>http://repo.maven.apache.org/maven2/org/apache/tomcat/tomcat/8.5.42/tomcat-8.5.42.zip</containerUrl>
      <artifactInstaller>
        <groupId>org.apache.tomcat</groupId>
        <artifactId>tomcat</artifactId>
        <version>${tomcat.version}</version>
      </artifactInstaller>
    </container>
    <configuration>
      <type>standalone</type>
      <home>
        ${project.build.directory}/apache-tomcat-${tomcat.version}
      </home>
      <properties>
        <cargo.start.jvmargs>
          -Xdebug
          -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
          -Xnoagent
          -Dorg.apache.tomcat.websocket.executorCoreSize=10
          -Dorg.apache.tomcat.websocket.executorMaxSize=15
          -Djava.compiler=NONE
        </cargo.start.jvmargs>
        <cargo.logging>medium</cargo.logging>
        <cargo.servlet.port>8443</cargo.servlet.port>
        <cargo.protocol>https</cargo.protocol>
        <cargo.tomcat.connector.clientAuth>false</cargo.tomcat.connector.clientAuth>
        <cargo.tomcat.connector.keyAlias>serverca</cargo.tomcat.connector.keyAlias>
        <cargo.tomcat.connector.keystoreFile>${project.basedir}/src/main/resources/local-keystore.jks</cargo.tomcat.connector.keystoreFile>
        <cargo.tomcat.connector.keystorePass>servercapass</cargo.tomcat.connector.keystorePass>
        <cargo.tomcat.connector.keystoreType>JKS</cargo.tomcat.connector.keystoreType>
        <cargo.tomcat.connector.sslProtocol>TLS</cargo.tomcat.connector.sslProtocol>
        <cargo.tomcat.httpSecure>true</cargo.tomcat.httpSecure>
      </properties>
    </configuration>
    <deployables>
      <deployable>
        <groupId>${project.groupId}</groupId>
        <artifactId>${project.artifactId}</artifactId>
        <type>war</type>
        <properties>
          <context>/signaling</context>
        </properties>
      </deployable>
    </deployables>
  </configuration>
</plugin>
```  

#### Access
- From your client app access it via:
	- [wss://127.0.0.1:8443/signaling/s-insecure](wss://127.0.0.1:8443/signaling/v1/s-insecure)
- Or the secured endpoint which after HTTP Upgrade to Websocket it expects and validates headers *vcuser* and *vctoken*:
	- [wss://127.0.0.1:8443/signaling/s](wss://127.0.0.1:8443/signaling/s)
See **NextRTC Video Chat exmaple** section.


## Spring Boot Standalone WAR:

#### Maven pom and Spring Bean Configuration setup
- On *pom.xml*:
	- remove next dependencies (if exist):
		```xml
		<groupId>javax.websocket</groupId>
		<artifactId>javax.websocket-api</artifactId>
		
		<groupId>javax.servlet</groupId>
		<artifactId>javax.servlet-api</artifactId>
		
		<groupId>javax.servlet</groupId>
		<artifactId>jstl</artifactId>
		```
	- add next depedency
		```xml
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-websocket</artifactId>
		</dependency>
		```
	- add next build plugins:
		```xml
		<plugin>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-maven-plugin</artifactId>
		</plugin>
        ```
	- remove Cargo plugin (if exist):
		```xml
		<groupId>org.codehaus.cargo</groupId>
		<artifactId>cargo-maven2-plugin</artifactId>
		```
- Then declare next beans in *SignalingConfiguration* class:
	```java
	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
	```
- Edit *application.properties* accordingly. Be aware *server.port* value is *8443*.
- Run:
```sh
mvn clean package
java -jar target/signaling.war
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



## jdeps on a Spring Boot Fat WAR file

Use `jdeps` to know which java modules the final application needs to run. Note that we are using `--multi-release=11`.

- *NOTE*: *this guide is only valid for Spring Boot fat WAR due to internal WAR structure. For a fat JAR package you will need to make some adjustments.*

- Windows:
```bash
mkdir target\docker-workdir
cd target\docker-workdir && jar -xf ..\signaling.war && cd ..\..
jdeps --add-modules=ALL-MODULE-PATH --ignore-missing-deps --multi-release=11 --print-module-deps ^
  -cp target\docker-workdir\WEB-INF\lib\*;target\docker-workdir\WEB-INF\lib-provided\* target\docker-workdir\WEB-INF\classes
```

- Linux:
```bash
mkdir target\docker-workdir
cd target\docker-workdir && jar -xf ..\signaling.war && cd ..\..
jdeps --add-modules=ALL-MODULE-PATH --ignore-missing-deps --multi-release=11 --print-module-deps \
  -cp target/docker-workdir/WEB-INF/lib/*;target/docker-workdir/WEB-INF/lib-provided/* target/docker-workdir/WEB-INF/classes
```

- Example Output:
```bash
java.base,java.compiler,java.desktop,java.instrument,java.management.rmi,java.naming,java.prefs,java.scripting,java.security.jgss,java.sql,jdk.httpserver,jdk.unsupported
```


## Run with Docker and test Distributed Event Bus with Hazelcast

- *NOTE*: this guide is only valid for Spring Boot Fat WAR due to internal WAR structure. If it's a JAR then modify scripts accordingly.

- First pack the Signaling Server in a Fat WAR artifact using Spring Boot maven plugin:
```bash
mvn clean package -P local,eventbus-hazelcast,java11
```

- **Create a multi layer Docker image for Spring Boot app**:
In order to take advantage of less frequency changes the [Dockerfile.java8](src/main/docker/Dockerfile.java8) file and 
[Dockerfile.java11](src/main/docker/Dockerfile.java11) file both define a multi layer image, so next time image build is 
fired it only updates application code.  
Script **docker-build.<bat|sh>** is moved to `target` folder during maven life cycle.  
It decompress the WAR file and creates the multi layer Docker image.  
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

## Native Image generation with GraalVM
**NOTE**: work in progress due to logback logging api issue and hazelcast instance node creation (issue)(https://github.com/oracle/graal/issues/1508) on image build time phase.  
**NOTE**: currently targeting graalvm 19.2.0.1.  
- You first need to build the signaling project and generate the WAR artifact for *java8* or *java11* depending on what graalvm installation you are targeting.
  - `mvn clean package -P local,eventbus-hazelcast,java8 -Dskip.docker.build=true`
- Locate at project root dir and download the [Spring-Graal-Native-Image](https://github.com/spring-projects-experimental/spring-graal-native.git) project:  
(Next scripts will clone it under target folder)
```bash
Windows:
  clone-spring-graal-native.bat
Linux
  clone-spring-graal-native.sh
```
- Generate native image from WAR artifact (**you will need 6GB of free memory!**):  
**Note** that Signaling WAR file contains `META-INF/native-image/org.fabri1983.signaling/native-image.properties` with all the options/flags.
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
