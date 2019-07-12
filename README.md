# Signaling Server using Spring Boot Websockets
Based on NextRTC Java project [NextRTC Signlaing Server](https://github.com/mslosarz/nextrtc-signaling-server)


## Create self signed certificate
*(skip this step if you already have your own certificate in your keystore)*

Enter to directory ```src/main/resources``` and generate self signed certificate (current certificate might be expired!):
```bash
keytool -genkey -alias tomcat -keyalg RSA -keystore keystore.jks
```
```
Enter keystore password: changeit
Re-enter new password: changeit
What is your first and last name?
  [Unknown]:  Local Name
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


## Create key pair for JWT
Enter to directory ```src/main/resources/profiles```.
	```sh
	openssl genrsa -out jwt_local_private.key 2048
	
	Convert private Key to PKCS#8 format (so Java can read it)
	openssl pkcs8 -topk8 -inform PEM -outform DER -in jwt_local_private.key -out jwt_local_private.der -nocrypt
	
	Output public key portion in DER format (so Java can read it)
	openssl rsa -in jwt_local_private.key -pubout -outform DER -out jwt_local_public.der
	
	rm -f jwt_local_private.key
	```

## Maven Profiles
**Profiles**
	- ```local``` (active by default)
	They set JWT Signer's private and public keys.

**Additional profiles**
	- ```eventbus-local``` (active by default)
	- ```eventbus-dist```
	They remove/add additional dependencies and disable/enable a Spring profile which allow the use of a distributed eventbus.
	When using *eventbus-local* some dependencies are removed and the beans defined in ```DistributedSignalingConfiguration``` are not created.
	When using *eventbus-dist* the opposite occurs.


## Build WAR file to deploy in Tomcat (external or with Cargo plugin)

#### Maven pom and Spring Bean Configuration setup
- Edit pom.xml:
	- add next property:
		```<javax.websocket.api.version>1.1</javax.websocket.api.version>```
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
		<groupId>org.apache.maven.plugins</groupId>
		<artifactId>maven-war-plugin</artifactId>
		
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
		<version>1.6.6</version>
		<configuration>
			<container>
				<containerId>tomcat8x</containerId>
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
					<cargo.tomcat.connector.keystoreFile>${project.basedir}/conf/keystore.jks
					</cargo.tomcat.connector.keystoreFile>
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
	- [wss://localhost:8443/signaling/s-insecure](wss://localhost:8443/signaling/v1/s-insecure)
- Or the secured endpoint which after HTTP Upgrade to Websocket it expects and validates headers *vcuser* and *vctoken*:
	- [wss://localhost:8443/signaling/s](wss://localhost:8443/signaling/s)


## Spring Boot standalone:

#### Maven pom and Spring Bean Configuration setup
- On *pom.xml*:
	- remove next dependencies (if exist):
		```xml
		<groupId>javax.websocket</groupId>
		<artifactId>javax.websocket-api</artifactId>
		
		<groupId>javax.servlet</groupId>
		<artifactId>javax.servlet-api</artifactId>
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
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-war-plugin</artifactId>
		</plugin>
		<plugin>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-maven-plugin</artifactId>
		</plugin>
        ```
	- remove Cargo plugin (if exist)
- Then declare next beans in *SignalingConfiguration* class:
	```java
	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
	```
- Edit *application.properties* accordingly. Be aware *server.port* value is *8432*.
- Run:
	- ```mvn clean install && java -jar target/signaling.war```
	
#### Access
- From your client app access it via:
	- [wss://localhost:8432/signaling/v1/s-insecure](wss://localhost:8432/signaling/v1/s-insecure)
- Or the secured endpoint which after HTTP Upgrade to Websocket it expects and validates headers *vcuser* and *vctoken*:
	- [wss://localhost:8432/signaling/v1/s](wss://localhost:8432/signaling/v1/s)
