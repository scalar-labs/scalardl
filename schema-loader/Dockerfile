ARG SCALARDB_VERSION

FROM ghcr.io/scalar-labs/scalardb-schema-loader:$SCALARDB_VERSION

# Creating a directory explicitly instead of using WORKDIR to create a directory is recommended way in Dockerfile
# https://github.com/moby/moby/issues/36677
USER 0
RUN mkdir /scalardl-schema-loader

USER 201:201
WORKDIR /scalardl-schema-loader

# 'ledger' and 'auditor' can be specified
ENV SCHEMA_TYPE='ledger'

COPY ledger-schema.json \
     auditor-schema.json \
     entrypoint.sh \
     database.properties \
     ./

ENTRYPOINT ["./entrypoint.sh"]
