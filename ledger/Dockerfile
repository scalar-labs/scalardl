# grpc_health_probe for Kubernetes:
# - https://kubernetes.io/blog/2018/10/01/health-checking-grpc-servers-on-kubernetes/
# You can confirm the digest with:
#   docker buildx imagetools inspect ghcr.io/grpc-ecosystem/grpc-health-probe:<TAG>
FROM ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.61@sha256:9df7b8dfbaf01d20eaedcd6de084c557074046f87cfc0413b7939ae18087c249 AS grpc-health-probe

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

COPY --from=grpc-health-probe /ko-app/grpc-health-probe /usr/local/bin/grpc_health_probe

WORKDIR /scalar

# The path should be relative from build/docker. The Gradle task
# copyFilesToDockerBuildContextDir copies this Dockerfile and ledger.tar to
# build/docker before building the image.
ADD ./ledger.tar .

COPY ledger.properties \
     log4j2.properties \
     docker-entrypoint.sh \
     ./ledger/

USER 201:201

ENV JAVA_OPT_MAX_RAM_PERCENTAGE='60.0' \
    JAVA_OPTS='-Dlog4j.configurationFile=file:/scalar/ledger/log4j2.properties'

ARG IMAGE_NAME
ARG IMAGE_VERSION
ARG IMAGE_RELEASE

LABEL name="${IMAGE_NAME}" \
      maintainer="Scalar, Inc." \
      vendor="Scalar, Inc." \
      version="${IMAGE_VERSION}" \
      release="${IMAGE_RELEASE}" \
      summary="ScalarDL Ledger Container Image" \
      description="ScalarDL is middleware for realizing a tamper-evident database system by detecting arbitrary faults (that is, Byzantine faults), such as data tampering and malicious attacks, in transactional database systems. It achieves such detection in a scalable and practical way like no other with its novel consensus algorithm."

WORKDIR /scalar/ledger

ENTRYPOINT ["./docker-entrypoint.sh"]

CMD ["./bin/scalar-ledger", "--config=ledger.properties"]

EXPOSE 50051
