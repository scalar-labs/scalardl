FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalar \
 && useradd -r --uid 201 -g scalar scalar

WORKDIR /scalar

# The path should be relative from build/docker. The Gradle task
# copyFilesToDockerBuildContextDir copies this Dockerfile and client.tar to
# build/docker before building the image.
ADD --chown=201:201 ./client.tar .

COPY --chown=201:201 \
     client.properties \
     log4j2.properties \
     ./

USER 201

ENV JAVA_OPTS='-Dlog4j.configurationFile=file:log4j2.properties'

CMD ["tail", "-f", "/dev/null"]
