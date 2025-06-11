FROM docker.io/busybox:1.36 AS tools

ENV DOCKERIZE_VERSION v0.6.1

# Install dockerize
RUN set -x && \
    wget -q "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    tar -xzvf "dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    ./dockerize --version

FROM eclipse-temurin:8-jre-jammy

RUN apt-get update && apt-get upgrade -y --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*

COPY --from=tools dockerize /usr/local/bin/

WORKDIR /scalar

# The path should be relative from build/docker. Running `gradle build`
# (provided by com.palantir.docker plugin) will copy this Dockerfile and
# ledger.tar to build/docker.
ADD ./client.tar .

COPY client.properties.tmpl .
COPY log4j2.properties.tmpl .

RUN groupadd -r --gid 201 scalar && \
    useradd -r --uid 201 -g scalar scalar
RUN chown -R scalar:scalar /scalar

USER 201

ENV JAVA_OPTS -Dlog4j.configurationFile=file:/tmp/log4j2.properties

CMD ["dockerize", "-template", "client.properties.tmpl:/tmp/client.properties", \
    "-template", "log4j2.properties.tmpl:/tmp/log4j2.properties", \
    "tail", "-f", "/dev/null"]
