FROM docker.io/busybox:1.36 AS tools

ENV GRPC_HEALTH_PROBE_VERSION=v0.4.46

# Install grpc_health_probe for kubernetes.
# https://kubernetes.io/blog/2018/10/01/health-checking-grpc-servers-on-kubernetes/
RUN set -x && \
    wget -q -O grpc_health_probe "https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64" && \
    chmod +x grpc_health_probe

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get upgrade -y --no-install-recommends \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r --gid 201 scalar \
 && useradd -r --uid 201 -g scalar scalar

COPY --from=tools grpc_health_probe /usr/local/bin/

WORKDIR /scalar

# The path should be relative from build/docker. Running `gradle build`
# (provided by com.palantir.docker plugin) will copy this Dockerfile and
# ledger.tar to build/docker.
ADD --chown=201:201 ./ledger.tar .

COPY --chown=201:201 \
     ledger.properties \
     log4j2.properties \
     docker-entrypoint.sh \
     ./ledger/

USER 201

ENV JAVA_OPT_MAX_RAM_PERCENTAGE='60.0' \
    JAVA_OPTS='-Dlog4j.configurationFile=file:log4j2.properties'

WORKDIR /scalar/ledger

ENTRYPOINT ["./docker-entrypoint.sh"]

CMD ["./bin/scalar-ledger", "--config=ledger.properties"]

EXPOSE 50051
