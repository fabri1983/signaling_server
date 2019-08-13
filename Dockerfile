FROM openjdk:12-jdk-alpine as staging
ARG DEPENDENCIES=target/dependencies
ARG JAVA_MAIN_CLASS

MAINTAINER fabri1983dockerid

VOLUME /tmp

RUN mkdir -p /staging/app/

# Stage dependencies
COPY ${DEPENDENCIES}/BOOT-INF/lib      /staging/app/BOOT-INF/lib
COPY ${DEPENDENCIES}/META-INF          /staging/app/META-INF
COPY ${DEPENDENCIES}/BOOT-INF/classes  /staging/app/BOOT-INF/classes


FROM openjdk:12-jdk-alpine

VOLUME /tmp

ENV JAVA_MODULES_HAZELCAST="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

# Create the individual layers
COPY --from=staging /staging/app/BOOT-INF/lib      /app/lib
COPY --from=staging /staging/app/META-INF          /app/META-INF
COPY --from=staging /staging/app/BOOT-INF/classes  /app

# You can test next entrypoint unzipping the signaling.jar file into folder target/signaling
# and then run: java -cp "target/signaling/BOOT-INF/classes:target/signaling/BOOT-INF/lib/*" org.fabri1983.signaling.entrypoint.SignalingEntryPoint
# Windows: ; is the classpath entries separator
# Linux: : is the classpath entries separator
# NOTE: the use of wildcard * only considers jar files, otherwise only includes class files.
ENTRYPOINT ["java","${JAVA_MODULES_HAZELCAST}","-cp","app:app/lib/*","${JAVA_MAIN_CLASS}"]
