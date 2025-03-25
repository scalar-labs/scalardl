FROM docker.io/busybox:1.36 AS tools

ENV DOCKERIZE_VERSION v0.6.1
ENV GRPC_HEALTH_PROBE_VERSION v0.4.37

# Install dockerize
RUN set -x && \
    wget -q "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    tar -xzvf "dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    ./dockerize --version

# Install grpc_health_probe for kubernetes.
# https://kubernetes.io/blog/2018/10/01/health-checking-grpc-servers-on-kubernetes/
RUN set -x && \
    wget -q -O grpc_health_probe "https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64" && \
    chmod +x grpc_health_probe

FROM eclipse-temurin:8-jre-jammy

RUN apt-get update && apt-get upgrade -y \
 && rm -rf /var/lib/apt/lists/*

COPY --from=tools dockerize /usr/local/bin/
COPY --from=tools grpc_health_probe /usr/local/bin/

WORKDIR /scalar

# The path should be relative from build/docker. Running `gradle build`
# (provided by com.palantir.docker plugin) will copy this Dockerfile and
# ledger.tar to build/docker.
ADD ./ledger.tar .

ENV SCALAR_DB_CONTACT_POINTS 'cassandra'
ENV SCALAR_DB_CONTACT_PORT '9042'
ENV SCALAR_DB_USERNAME ''
ENV SCALAR_DB_PASSWORD ''
ENV SCALAR_DB_STORAGE 'cassandra'
ENV SCALAR_DL_LEDGER_FUNCTION_ENABLED 'true'
ENV SCALAR_DL_LEDGER_NONCE_TXID_ENABLED 'true'

# it needs to CD to ledger to see security.policy
WORKDIR /scalar/ledger

COPY ledger.properties.tmpl .
COPY log4j2.properties.tmpl .
COPY docker-entrypoint.sh .

RUN groupadd -r --gid 201 scalar && \
    useradd -r --uid 201 -g scalar scalar
RUN chown -R scalar:scalar /scalar/ledger

USER 201

ENV JAVA_OPT_MAX_RAM_PERCENTAGE '60.0'
ENV JAVA_OPTS '-Dlog4j.configurationFile=file:log4j2.properties'

ENTRYPOINT ["./docker-entrypoint.sh"]

CMD ["dockerize", "-template", "ledger.properties.tmpl:ledger.properties", \
    "-template", "log4j2.properties.tmpl:log4j2.properties", \
    "./bin/scalar-ledger", "--config=ledger.properties"]

EXPOSE 50051
