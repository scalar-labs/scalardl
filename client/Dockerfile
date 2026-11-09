FROM docker.io/busybox:1.36 AS tools

ENV DOCKERIZE_VERSION v0.6.1

# Install dockerize
RUN set -x && \
    wget -q "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    tar -xzvf "dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    ./dockerize --version

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
