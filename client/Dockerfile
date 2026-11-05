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
