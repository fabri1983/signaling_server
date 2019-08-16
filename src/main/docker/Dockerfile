FROM adoptopenjdk/openjdk12:alpine-jre AS STAGING
ARG DEPENDENCIES=docker-workdir

RUN mkdir -p /staging/app/

# Stage dependencies and classes
COPY ${DEPENDENCIES}/js                   /staging/app/js
COPY ${DEPENDENCIES}/META-INF             /staging/app/META-INF
COPY ${DEPENDENCIES}/WEB-INF              /staging/app/WEB-INF
COPY ${DEPENDENCIES}/callTo.html          /staging/app/
COPY ${DEPENDENCIES}/general-error.jsp    /staging/app/
COPY ${DEPENDENCIES}/index.html           /staging/app/
COPY ${DEPENDENCIES}/videochat.html       /staging/app/

# Java 12: add self-signed certificate into Java 12 keystore
#RUN keytool -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -trustcacerts 
#  -importcert -alias tomcat -file /staging/app/signaling-self.crt


FROM adoptopenjdk/openjdk12:alpine-jre
ARG JAVA_MAIN_CLASS

ENV ENV_JAVA_MAIN_CLASS=${JAVA_MAIN_CLASS} \
    ENV_JAVA_MODULES_FOR_HAZELCAST="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

# Create the individual layers
COPY --from=STAGING /staging/app  /app

# Entry with exec so jvm flags are correctly gathered
ENTRYPOINT exec java ${ENV_JAVA_MODULES_FOR_HAZELCAST} -cp /app/WEB-INF/classes:/app/WEB-INF/lib/*:/app/WEB-INF/lib-provided/* ${ENV_JAVA_MAIN_CLASS}
