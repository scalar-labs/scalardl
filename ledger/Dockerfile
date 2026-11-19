# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:21.0.13_9-jre-resolute@sha256:556903b1f40c0a0ddc0bd05cf413436a3b2ce48c9ee19fc199434898fd3ea2b9

RUN export DEBIAN_FRONTEND=noninteractive \
 && apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalar \
 && useradd -r --uid 201 -g scalar -s /usr/sbin/nologin scalar

# Install grpc_health_probe for kubernetes:
# - https://kubernetes.io/blog/2018/10/01/health-checking-grpc-servers-on-kubernetes/
# You can confirm the checksum value in "checksums.txt" in:
# - https://github.com/grpc-ecosystem/grpc-health-probe/releases
ARG GRPC_HEALTH_PROBE_VERSION=v0.4.61
ARG GRPC_HEALTH_PROBE_SHA256=3746f63e89561616ce69eafdc9b46f8b23b47db3a564f6f2e8c31359a60e2f38
ADD --chmod=755 \
    --checksum=sha256:${GRPC_HEALTH_PROBE_SHA256} \
    "https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64" \
    /usr/local/bin/grpc_health_probe

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
