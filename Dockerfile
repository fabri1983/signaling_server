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

# Create the individual layers
COPY --from=staging /staging/app/BOOT-INF/lib      /app/lib
COPY --from=staging /staging/app/META-INF          /app/META-INF
COPY --from=staging /staging/app/BOOT-INF/classes  /app

# You can test next entrypoint unzipping the signaling.jar file into folder target/signaling
# and then run: java -cp "target/signaling/BOOT-INF/classes:target/signaling/BOOT-INF/lib/*" org.fabri1983.signaling.entrypoint.SignalingEntryPoint
# Windows: ; is the classpath entries separator
# Linux: : is the classpath entries separator
# NOTE: the use of wildcard * only considers jar files, otherwise only includes class files.
ENTRYPOINT ["java","-cp","app:app/lib/*","${JAVA_MAIN_CLASS}"]
