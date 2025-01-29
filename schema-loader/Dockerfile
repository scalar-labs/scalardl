ARG SCALARDB_VERSION

FROM ghcr.io/scalar-labs/scalardb-schema-loader:$SCALARDB_VERSION

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
COPY --chown=201:201 database.properties .

ENTRYPOINT ["./entrypoint.sh"]
