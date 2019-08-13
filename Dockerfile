FROM openjdk:12-jdk-alpine as staging
ARG DEPENDENCIES=target/docker-dependencies

VOLUME /tmp

RUN mkdir -p /staging/app/

# Stage dependencies and classes
COPY ${DEPENDENCIES}/BOOT-INF/lib      /staging/app/BOOT-INF/lib
COPY ${DEPENDENCIES}/META-INF          /staging/app/META-INF
COPY ${DEPENDENCIES}/BOOT-INF/classes  /staging/app/BOOT-INF/classes


FROM openjdk:12-jdk-alpine
ARG JAVA_MAIN_CLASS

ENV ENV_JAVA_MAIN_CLASS=${JAVA_MAIN_CLASS}
ENV ENV_JAVA_MODULES_HAZELCAST="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

VOLUME /tmp

# Create the individual layers
COPY --from=staging /staging/app/BOOT-INF/lib      /app/lib
COPY --from=staging /staging/app/META-INF          /app/META-INF
COPY --from=staging /staging/app/BOOT-INF/classes  /app

#ENTRYPOINT ["java", ${ENV_JAVA_MODULES_HAZELCAST}, "-cp","app:app/lib/*", ${ENV_JAVA_MAIN_CLASS}]
ENTRYPOINT ["java", "-cp","app:app/lib/*", "org.fabri1983.signaling.entrypoint.SignalingEntryPoint"]