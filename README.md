# Signaling Server with Spring Boot Websockets and Docker

[![Build Status](https://travis-ci.org/fabri1983/signaling_server.svg?branch=master)](https://travis-ci.org/fabri1983/signaling_server?branch=master)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Coverage Status](https://coveralls.io/repos/github/fabri1983/signaling_server/badge.svg)](https://coveralls.io/github/fabri1983/signaling_server?branch=master)
&nbsp;&nbsp;&nbsp;&nbsp;
[![Code Climate](https://codeclimate.com/github/fabri1983/signaling_server/badges/gpa.svg)](https://codeclimate.com/github/fabri1983/signaling_server)


This project uses [NextRTC Signaling Server](https://github.com/mslosarz/nextrtc-signaling-server) project.
I added custom signals to provide a complete video call solution between two clients.
It has a distributed event bus so the signaling server can be deployed in a cluster with auto discovery.


- Runs on **Java 12**. If you want to use Java 8 then you need to:
	- change [Dockerfile](src/main/docker/Dockerfile):
		- reflect the location of the *jre keystore*. **NOTE**: import cert command is commented out because this project uses custom keystore.jks.
		- remove any use of ${ENV_JAVA_MODULES_FOR_HAZELCAST}
	- edit *pom.xml* ```<properties>``` section:
		- change ```<java.version>``` and ```<maven.compiler.target>``` 
		- remove ```<maven.compiler.release>```
- Uses Maven 3.6.x
- After Spring Boot repackages the final *WAR* file, a Docker image is built. So you need to get Docker installed and running. 
If not installed then se ```-Dskip.docker.build=true``` to skip the docker build.


## Create self signed certificate
*(skip this step if you already have your own certificate in your keystore, but do not forget to edit application.properties and docker-compose-local.yml files accordingly)*

Enter to directory ```src/main/resources``` and generate self-signed certificate into custom keystore (current certificate might be expired!):
```bash
keytool -genkey -alias tomcat -keyalg RSA -keystore keystore.jks -validity 365 -keysize 2048
```
```bash
Enter keystore password: changeit
Re-enter new password: changeit
What is your first and last name?
  [Unknown]:  127.0.0.1
What is the name of your organizational unit?
  [Unknown]:  Engineering
What is the name of your organization?
  [Unknown]:  Fabri Corp
What is the name of your City or Locality?
  [Unknown]:  CABA
What is the name of your State or Province?
  [Unknown]:  Buenos Aires
What is the two-letter country code for this unit?
  [Unknown]:  AR
Is CN=Local Name, OU=Engineering, O=Fabri Corp, L=CABA, ST=Buenos Aires, C=AR correct?
  [no]: yes

Enter key password for <tomcat>
  (RETURN if same as keystore password): <RETURN>
```

Export certificate file in X.509 for Dockerfile internal command:
```bash
keytool -export -rfc -alias tomcat -file signaling-self.crt -keystore keystore.jks
```
 
Edit *application.properties* accordingly if you change any of above information.


## Create key pair for JWT (Json Web Token)
Enter to directory ```src/main/resources/profiles```.
```bash
openssl genrsa -out jwt_local_private.key 2048
```
Convert private Key to PKCS#8 format (so Java can read it)
```bash
openssl pkcs8 -topk8 -inform PEM -outform DER -in jwt_local_private.key -out jwt_local_private.der -nocrypt
```
Output public key portion in DER format (so Java can read it)
```bash
openssl rsa -in jwt_local_private.key -pubout -outform DER -out jwt_local_public.der
rm -f jwt_local_private.key
```

## Maven Profiles
**Profiles**  
- ```local``` (active by default)  
Set JWT's signer and verifier private and public keys.

**Additional profiles**  
- ```eventbus-local``` (active by default)  
- ```eventbus-hazelcast```  
Remove/add additional dependencies and disable/enable a Spring profile which allow the use of a distributed eventbus.
When using *eventbus-local* some dependencies are removed and the beans defined in ```DistributedSignalingConfiguration``` are not created.
When using *eventbus-hazelcast* the opposite occurs.


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
	- ```ServerEndpointExporter serverEndpointExporter()```
- Edit *application.properties* accordingly. Be aware *server.port* value is *8443*.

#### Eclipse IDE
- Import as Maven project
- Configure Dynamic Web project: *Properties -> Project Facets*
- Set context root to **signaling**: 
	- *Properties -> Web Project Settings*
- Active profiles: using ```ALT+SHIFT+P``` select *local* and *eventbus-local* profiles.
- Build project: ```CTRL+B```

#### Deploy on external Tomcat installation
- Use next configuration on your *<CATALINA_BASE>/conf/server.xml*:
	```xml
	<Connector port="8443" protocol="org.apache.coyote.http11.Http11Nio2Protocol" 
		SSLEnabled="true" clientAuth="false" keyAlias="tomcat" keystoreFile="conf/keystore.jks" 
		keystorePass="changeit" scheme="https" secure="true" sslProtocol="TLS" />
	```
	- Don't forget to copy the certificate into *<CATALINA_BASE>/conf/* folder.
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
        <cargo.tomcat.connector.keyAlias>tomcat</cargo.tomcat.connector.keyAlias>
        <cargo.tomcat.connector.keystoreFile>${project.basedir}/conf/keystore.jks</cargo.tomcat.connector.keystoreFile>
        <cargo.tomcat.connector.keystorePass>changeit</cargo.tomcat.connector.keystorePass>
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


## Spring Boot standalone jar:

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
	- ```mvn clean package && java -jar target/signaling.war```
	
#### Access
- From your client app access it via:
	- [wss://127.0.0.1:8443/signaling/v1/s-insecure](wss://127.0.0.1:8443/signaling/v1/s-insecure)
- Or the secured endpoint which after HTTP Upgrade to Websocket it expects and validates headers *vcuser* and *vctoken*:
	- [wss://127.0.0.1:8443/signaling/v1/s](wss://127.0.0.1:8443/signaling/v1/s)
See **NextRTC Video Chat exmaple** section.


## NextRTC Video Chat usage
- Enter [https://127.0.0.1:8443/signaling/videochat.html](https://127.0.0.1:8443/signaling/videochat.html) in your favourite browser
(**https** is important, because default http handler isn't configured).
* Accept untrusted certificate.
* You can use query param *forceTurn=true* in order to force relay ICE Transport Policy and so test your TURN server:
[https://127.0.0.1:8443/signaling/videochat.html?forceTurn=true](https://127.0.0.1:8443/signaling/videochat.html?forceTurn=true)

_Sometimes websocket (js side) is throwing an exception and can't connect via websocket o signiling server, then try to change localhost to 127.0.0.1_

This is a working test of the Signaling Server and the videochat client using a Chrome tab on my laptop and an Opera tab on my mobile phone. 
Server exposed with [ngrok](https://ngrok.com/).

![videochat with local signaling](/videochat_example_ngrok.jpg?raw=true "videochat with local signaling")


## Run with Docker and test Distributed Event Bus with Hazelcast
- First pack the Signaling Server in a fat jar using default Spring Boot maven plugins:
```bash
mvn clean package -P local,eventbus-hazelcast
```

- **Create a multi layer Docker image for Spring Boot app**:
In order to take advantage of less frequency changes the [Dockerfile](src/main/docker/Dockerfile) defines a multi layer image, 
so next time image build is fired it only updates application code.  
Script **docker-build.<bat|sh>** is moved to ```target``` folder after repackage is done.  
It decompress the war file and creates the multi layer Docker image.  
Keep an eye on the context size sent to Docker's context:
```bash
Sending build context to Docker daemon  35.08MB
```  
Once the image build finishes use next command to check layers size:
```bash
docker history fabri1983dockerid/signaling-server:dev
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
docker container run -i -p 8481:8443 --name signaling-server-1 fabri1983dockerid/signaling-server:dev
docker container run -i -p 8482:8443 --name signaling-server-2 fabri1983dockerid/signaling-server:dev
(replace -i by -d if you want to detach the process and let it run on background)
```
Then manage it with:
```bash
docker container stop|start <container-name>
```
Or you can use the [docker-compose-local.yml](src/main/docker/docker-compose-local.yml):
```bash
docker-compose -f src/main/docker/docker-compose-local.yml up
```
Then manage it with:
```bash
docker-compose -f src/main/docker/docker-compose-local.yml stop|start
```

- Test the Distributed Event Bus with Hazelcast:
(**NOTE**: work in progress due to serialization issues)
	- If you are using docker in **Windows** with **Docker Tool Box** then visit:
		- [videochat-1](https://192.168.99.100:8481/signaling/videochat.html)
		- [videochat-2](https://192.168.99.100:8482/signaling/videochat.html)
	- If on **Linux**:
		- [videochat-1](https://172.17.0.2:8481/signaling/videochat.html)
		- [videochat-2](https://172.17.0.3:8482/signaling/videochat.html)
		- or get the running Docker ip:
		```bash
		docker inspect -f "{{ .NetworkSettings.IPAddress }}" <containerNameOrId>
		```
