# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/busybox/tags
FROM docker.io/busybox:1.36.1@sha256:73aaf090f3d85aa34ee199857f03fa3a95c8ede2ffd4cc2cdb5b94e566b11662 AS tools

# Install dockerize
# You can confirm the checksum value in "checksums.txt" in:
# - https://github.com/jwilder/dockerize/releases
ARG DOCKERIZE_VERSION=v0.16.0
ARG DOCKERIZE_SHA256=1c6d8cf10cfd764fbb58f4600b477bd4f90d65b5505fca2e3474f3176756f434
ADD --checksum=sha256:${DOCKERIZE_SHA256} \
    "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" \
    /tmp/dockerize.tar.gz
RUN tar -xzf /tmp/dockerize.tar.gz dockerize \
 && ./dockerize --version

# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:8u504-b01-jre-resolute@sha256:1b14bd3d3ccdae3435e24fb3203cc8d53566a2980d572bff271f067f304ef176

RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*

# Temporary fix for the emSign CA compromise (CVE-2026-91403 and CVE-2026-114820).
# No Temurin 8 release drops the compromised emSign root CAs yet, so we delete them
# from cacerts at build time. We can remove this RUN after we update to such a release.
RUN keytool -delete -keystore "${JAVA_HOME}/lib/security/cacerts" -storepass changeit -alias "cn=emsign_ecc_root_ca_-_g3,o=emudhra_technologies_limited,ou=emsign_pki,c=in" \
 && keytool -delete -keystore "${JAVA_HOME}/lib/security/cacerts" -storepass changeit -alias "cn=emsign_ecc_root_ca_-_c3,o=emudhra_inc,ou=emsign_pki,c=us" \
 && keytool -delete -keystore "${JAVA_HOME}/lib/security/cacerts" -storepass changeit -alias "cn=emsign_root_ca_-_c1,o=emudhra_inc,ou=emsign_pki,c=us"

COPY --chown=0:0 --from=tools dockerize /usr/local/bin/

WORKDIR /scalar

# The path should be relative from build/docker. Running `gradle build`
# (provided by com.palantir.docker plugin) will copy this Dockerfile and
# ledger.tar to build/docker.
ADD ./client.tar .

COPY client.properties.tmpl .
COPY log4j2.properties.tmpl .

RUN groupadd -r --gid 201 scalar && \
    useradd -r --uid 201 -g scalar -s /usr/sbin/nologin scalar

USER 201:201

ENV JAVA_OPTS -Dlog4j.configurationFile=file:/tmp/log4j2.properties

CMD ["dockerize", "-template", "client.properties.tmpl:/tmp/client.properties", \
    "-template", "log4j2.properties.tmpl:/tmp/log4j2.properties", \
    "tail", "-f", "/dev/null"]
