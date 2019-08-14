FROM adoptopenjdk/openjdk12:alpine-jre AS STAGING
ARG DEPENDENCIES=docker-workdir

VOLUME /tmp

RUN mkdir -p /staging/app/

# Stage dependencies and classes
COPY ${DEPENDENCIES}/WEB-INF/lib/*            /staging/app/lib/
COPY ${DEPENDENCIES}/WEB-INF/lib-provided/*   /staging/app/lib/
COPY ${DEPENDENCIES}/WEB-INF/classes/.        /staging/app/
COPY ${DEPENDENCIES}/js                       /staging/app/js
COPY ${DEPENDENCIES}/callTo.html              /staging/app/
COPY ${DEPENDENCIES}/general-error.jsp        /staging/app/
COPY ${DEPENDENCIES}/index.html               /staging/app/
COPY ${DEPENDENCIES}/videochat.html           /staging/app/

# Java 12: add self-signed certificate into Java 12 keystore
#RUN keytool -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -trustcacerts 
#  -importcert -alias tomcat -file /staging/app/signaling-self.crt


FROM adoptopenjdk/openjdk12:alpine-jre
ARG JAVA_MAIN_CLASS

ENV ENV_JAVA_MAIN_CLASS=${JAVA_MAIN_CLASS}
ENV ENV_JAVA_MODULES_HAZELCAST="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

VOLUME /tmp

# Create the individual layers
COPY --from=STAGING /staging/app   /app

# Entry with exec so jvm flags are correctly gathered
ENTRYPOINT exec java ${ENV_JAVA_MODULES_HAZELCAST} -cp /app:/app/lib/* ${ENV_JAVA_MAIN_CLASS}
