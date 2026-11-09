ARG SCALARDB_VERSION

# You can see sha256 hash in the following page:
# - https://hub.docker.com/_/busybox/tags
FROM docker.io/busybox:1.36.1@sha256:73aaf090f3d85aa34ee199857f03fa3a95c8ede2ffd4cc2cdb5b94e566b11662 AS tools

# Install dockerize
# Support for use cases where environment variables are used to configure the database
# You can confirm the checksum value in "checksums.txt" in:
# - https://github.com/jwilder/dockerize/releases
ARG DOCKERIZE_VERSION=v0.16.0
ARG DOCKERIZE_SHA256=1c6d8cf10cfd764fbb58f4600b477bd4f90d65b5505fca2e3474f3176756f434
ADD --checksum=sha256:${DOCKERIZE_SHA256} \
    "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" \
    /tmp/dockerize.tar.gz
RUN tar -xzf /tmp/dockerize.tar.gz dockerize \
 && ./dockerize --version

FROM ghcr.io/scalar-labs/scalardb-schema-loader:$SCALARDB_VERSION

COPY --chown=0:0 --from=tools dockerize /usr/local/bin/

# Creating a directory explicitly instead of using WORKDIR to create a directory is recommended way in Dockerfile
# https://github.com/moby/moby/issues/36677
USER 0
RUN mkdir /scalardl-schema-loader

USER 201:201
WORKDIR /scalardl-schema-loader

# 'ledger' and 'auditor' can be specified
ENV SCHEMA_TYPE 'ledger'

COPY ledger-schema.json .
COPY auditor-schema.json .
COPY entrypoint.sh .
COPY database.properties.tmpl .

ENTRYPOINT ["./entrypoint.sh"]
