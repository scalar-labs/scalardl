FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get upgrade -y --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /scalar

# The path should be relative from build/docker. Running `gradle build`
# (provided by com.palantir.docker plugin) will copy this Dockerfile and
# ledger.tar to build/docker.
ADD ./client.tar .

COPY client.properties.for.docker ./client.properties
COPY log4j2.properties .

RUN groupadd -r --gid 201 scalar && \
    useradd -r --uid 201 -g scalar scalar
RUN chown -R scalar:scalar /scalar

USER 201

ENV JAVA_OPTS -Dlog4j.configurationFile=file:log4j2.properties

CMD ["tail", "-f", "/dev/null"]
