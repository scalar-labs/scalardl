ARG SCALARDB_VERSION

FROM docker.io/busybox:1.36 AS tools

ENV DOCKERIZE_VERSION v0.6.1

# Install dockerize
# Support for use cases where environment variables are used to configure the database
RUN set -x && \
    wget -q "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    tar -xzvf "dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
    ./dockerize --version

FROM ghcr.io/scalar-labs/scalardb-schema-loader:$SCALARDB_VERSION

COPY --from=tools dockerize /usr/local/bin/

# Creating a directory and setting an owner explicitly instead of using WORKDIR to create a directory is recommended way in Dockerfile
# https://github.com/moby/moby/issues/36677
USER root
RUN mkdir /scalardl-schema-loader && chown 201:201 /scalardl-schema-loader

USER 201
WORKDIR /scalardl-schema-loader

# 'ledger' and 'auditor' can be specified
ENV SCHEMA_TYPE 'ledger'

COPY --chown=201:201 ledger-schema.json .
COPY --chown=201:201 auditor-schema.json .
COPY --chown=201:201 entrypoint.sh .
COPY --chown=201:201 database.properties.tmpl .

ENTRYPOINT ["./entrypoint.sh"]
