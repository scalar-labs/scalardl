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
FROM eclipse-temurin:21.0.13_9-jre-resolute@sha256:f8622d0220c9e218d7a33f24a7b9e1ad6bc726085f8b0ae74e1adf85f67f05c4

RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalar \
 && useradd -r --uid 201 -g scalar -s /usr/sbin/nologin scalar

COPY --chown=0:0 --from=tools dockerize /usr/local/bin/

WORKDIR /scalar

# The path should be relative from build/docker. The Gradle task
# copyFilesToDockerBuildContextDir copies this Dockerfile and client.tar to
# build/docker before building the image.
ADD ./client.tar .

COPY client.properties.tmpl \
     log4j2.properties.tmpl \
     ./

USER 201:201

ENV JAVA_OPTS='-Dlog4j.configurationFile=file:/tmp/log4j2.properties'

CMD ["dockerize", "-template", "client.properties.tmpl:/tmp/client.properties", \
    "-template", "log4j2.properties.tmpl:/tmp/log4j2.properties", \
    "tail", "-f", "/dev/null"]
