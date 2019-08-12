FROM openjdk:12-jdk-alpine as staging
ARG JAR_FILE
ARG JAVA_MAIN_CLASS

MAINTAINER fabri1983dockerid

# Install unzip; needed to unzip jar file
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

# Stage the fat JAR
COPY ${JAR_FILE} /staging/myFatApp.jar

# unzip thin app to avoid cache changes for new JAR
RUN mkdir /staging/myThinApp \
   && unzip -q /staging/myThinApp.jar -d /staging/myThinApp


FROM openjdk:12-jre-alpine

VOLUME /tmp

# Create the individual layers
COPY --from=staging /staging/myThinApp/BOOT-INF/lib /app/lib
COPY --from=staging /staging/myThinApp/META-INF /app/META-INF
COPY --from=staging /staging/myThinApp/BOOT-INF/classes /app

# You can test next entrypoint unzipping the signaling.jar file into folder target/signaling
# and then run: java -cp "target/signaling/BOOT-INF/classes:target/signaling/BOOT-INF/lib/*" org.fabri1983.signaling.entrypoint.SignalingEntryPoint
# Windows: ; is the classpath entries separator
# Linux: : is the classpath entries separator
# NOTE: the use of wildcard * only considers jar files.
ENTRYPOINT ["java","-cp","app:app/lib/*","${JAVA_MAIN_CLASS}"]
