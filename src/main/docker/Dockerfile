# OpenJDK 12 alpine jdk/jre/slim uses alpine 3.10.
# Visit https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/12/jre/alpine/Dockerfile.hotspot.releases.full

######################################################################################################
FROM adoptopenjdk/openjdk12:alpine AS STAGING-OPENJDK12-MINI

# At this point JAVA_HOME refers to /opt/java/openjdk

# Build a smaller Java 12 JRE
RUN jlink \
     --module-path ${JAVA_HOME}/jmods \
     --compress=2 \
     --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management.rmi,java.naming,java.prefs,java.scripting,java.security.jgss,java.sql,jdk.httpserver,jdk.unsupported \
     --no-header-files \
     --no-man-pages \
     --strip-debug \
     --output ${JAVA_HOME}/customjre


######################################################################################################
FROM alpine:3.10 AS STAGING-WAR
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


######################################################################################################
FROM alpine:3.10
ARG JAVA_MAIN_CLASS

ENV ENV_JAVA_MAIN_CLASS=${JAVA_MAIN_CLASS} \
    JAVA_HOME=/opt/java/openjdk \
    ENV_JAVA_MODULES_FOR_HAZELCAST="--add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED"

ENV PATH=$PATH:$JAVA_HOME/bin

# The JRE mini runtime we build in previous stage is based on glibc, and Alpine linux uses musl, 
# so we need to add glibc support:

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

RUN apk add --no-cache --virtual .build-deps curl binutils \
    && GLIBC_VER="2.29-r0" \
    && ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download" \
    && GCC_LIBS_URL="https://archive.archlinux.org/packages/g/gcc-libs/gcc-libs-9.1.0-2-x86_64.pkg.tar.xz" \
    && GCC_LIBS_SHA256="91dba90f3c20d32fcf7f1dbe91523653018aa0b8d2230b00f822f6722804cf08" \
    && ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.2.11-3-x86_64.pkg.tar.xz" \
    && ZLIB_SHA256=17aede0b9f8baa789c5aa3f358fbf8c68a5f1228c5e6cba1a5dd34102ef4d4e5 \
    && curl -LfsS https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub -o /etc/apk/keys/sgerrand.rsa.pub \
    && SGERRAND_RSA_SHA256="823b54589c93b02497f1ba4dc622eaef9c813e6b0f0ebbb2f771e32adf9f4ef2" \
    && echo "${SGERRAND_RSA_SHA256} */etc/apk/keys/sgerrand.rsa.pub" | sha256sum -c - \
    && curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-${GLIBC_VER}.apk > /tmp/glibc-${GLIBC_VER}.apk \
    && apk add /tmp/glibc-${GLIBC_VER}.apk \
    && curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-bin-${GLIBC_VER}.apk > /tmp/glibc-bin-${GLIBC_VER}.apk \
    && apk add /tmp/glibc-bin-${GLIBC_VER}.apk \
    && curl -Ls ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-i18n-${GLIBC_VER}.apk > /tmp/glibc-i18n-${GLIBC_VER}.apk \
    && apk add /tmp/glibc-i18n-${GLIBC_VER}.apk \
    && /usr/glibc-compat/bin/localedef --force --inputfile POSIX --charmap UTF-8 "$LANG" || true \
    && echo "export LANG=$LANG" > /etc/profile.d/locale.sh \
    && curl -LfsS ${GCC_LIBS_URL} -o /tmp/gcc-libs.tar.xz \
    && echo "${GCC_LIBS_SHA256} */tmp/gcc-libs.tar.xz" | sha256sum -c - \
    && mkdir /tmp/gcc \
    && tar -xf /tmp/gcc-libs.tar.xz -C /tmp/gcc \
    && mv /tmp/gcc/usr/lib/libgcc* /tmp/gcc/usr/lib/libstdc++* /usr/glibc-compat/lib \
    && strip /usr/glibc-compat/lib/libgcc_s.so.* /usr/glibc-compat/lib/libstdc++.so* \
    && curl -LfsS ${ZLIB_URL} -o /tmp/libz.tar.xz \
    && echo "${ZLIB_SHA256} */tmp/libz.tar.xz" | sha256sum -c - \
    && mkdir /tmp/libz \
    && tar -xf /tmp/libz.tar.xz -C /tmp/libz \
    && mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib \
    && apk del --purge .build-deps glibc-i18n \
    && rm -rf /tmp/*.apk /tmp/gcc /tmp/gcc-libs.tar.xz /tmp/libz /tmp/libz.tar.xz /var/cache/apk/*

# Copy JRE mini runtime from previous stage
COPY --from=STAGING-OPENJDK12-MINI $JAVA_HOME/customjre $JAVA_HOME

# Copy staged decompressed WAR from previous stage
COPY --from=STAGING-WAR /staging/app  /app

# Entry with exec so jvm flags are correctly gathered
ENTRYPOINT exec java ${ENV_JAVA_MODULES_FOR_HAZELCAST} -cp /app/WEB-INF/classes:/app/WEB-INF/lib/*:/app/WEB-INF/lib-provided/* ${ENV_JAVA_MAIN_CLASS}
