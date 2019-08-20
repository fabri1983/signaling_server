# OpenJDK 12 alpine jdk/jre uses alpine 3.10. See last FROM command.
# Visit https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/12/jre/alpine/Dockerfile.hotspot.releases.full

######################################################################################################
FROM alpine:3.10 AS STAGING-1
ARG DEPENDENCIES=docker-workdir

LABEL maintainer="fabri1983dockerid"

RUN mkdir -p /staging/app/

# Stage dependencies and classes
COPY ${DEPENDENCIES}/js                   /staging/app/js
COPY ${DEPENDENCIES}/META-INF             /staging/app/META-INF
COPY ${DEPENDENCIES}/WEB-INF              /staging/app/WEB-INF
COPY ${DEPENDENCIES}/callTo.html          /staging/app/
COPY ${DEPENDENCIES}/general-error.jsp    /staging/app/
COPY ${DEPENDENCIES}/index.html           /staging/app/
COPY ${DEPENDENCIES}/videochat.html       /staging/app/


######################################################################################################
FROM adoptopenjdk/openjdk12:alpine-jre
ARG JAVA_MAIN_CLASS

LABEL maintainer="fabri1983dockerid"

ENV ENV_JAVA_MAIN_CLASS=${JAVA_MAIN_CLASS} \
    ENV_JAVA_MODULES_FOR_HAZELCAST="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

# Create the individual layers
COPY --from=STAGING-1 /staging/app  /app

# Entry with exec so jvm flags are correctly gathered
ENTRYPOINT exec java ${ENV_JAVA_MODULES_FOR_HAZELCAST} -cp /app/WEB-INF/classes:/app/WEB-INF/lib/*:/app/WEB-INF/lib-provided/* ${ENV_JAVA_MAIN_CLASS}
