# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.13.1_2-jre-resolute@sha256:4249bd74371ea0e4b7d40da7f1d4354a894be80812abf7e4ea5bdd421dc3f6c8

RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalar \
 && useradd -r --uid 201 -g scalar -s /usr/sbin/nologin scalar

WORKDIR /scalar

# The path should be relative from build/docker. The Gradle task
# copyFilesToDockerBuildContextDir copies this Dockerfile and client.tar to
# build/docker before building the image.
ADD ./client.tar .

COPY client.properties \
     log4j2.properties \
     ./

USER 201:201

ENV JAVA_OPTS='-Dlog4j.configurationFile=file:/scalar/log4j2.properties'

CMD ["tail", "-f", "/dev/null"]
