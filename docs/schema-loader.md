# scalardl-schema-loader

A Docker image that loads the database schema with [Schema Tool for Scalar DB](https://github.com/scalar-labs/scalardb/tree/master/schema-loader/).

## How to Build

```console
docker build . -t ghcr.io/scalar-labs/scalardl-schema-loader:<version>
```

## How to Run

### For Cosmos DB

```console
docker run --rm [--env SCHEMA_TYPE=auditor] ghcr.io/scalar-labs/scalardl-schema-loader:<version> \
  --cosmos -h <YOUR_ACCOUNT_URI> -p <YOUR_ACCOUNT_PASSWORD> [-r BASE_RESOURCE_UNIT]
```

### For DynamoDB

```console
docker run --rm [--env SCHEMA_TYPE=auditor] ghcr.io/scalar-labs/scalardl-schema-loader:<version> \
  --dynamo --region <REGION> -u <ACCESS_KEY_ID> -p <SECRET_ACCESS_KEY> [-r BASE_RESOURCE_UNIT]
```

### For Cassandra

```console
docker run --rm [--env SCHEMA_TYPE=auditor] ghcr.io/scalar-labs/scalardl-schema-loader:<version> \
  --cassandra -h <CASSANDRA_IP> -u <CASSNDRA_USER> -p <CASSANDRA_PASSWORD> [-n <NETWORK_STRATEGY> -R <REPLICATION_FACTOR>]
```

### For using a config file

* For Ledger
  ```console
  docker run --rm \
    -v <PROPERTIES_FILE_PATH>:/scalardl-schema-loader/database.properties.tmpl \
    ghcr.io/scalar-labs/scalardl-schema-loader:<version> \
    --config database.properties --coordinator [<SOME_OPTIONS> [, ...]]
  ```

* For Auditor
  ```console
  docker run --rm --env SCHEMA_TYPE=auditor \
    -v <PROPERTIES_FILE_PATH>:/scalardl-schema-loader/database.properties.tmpl \
    ghcr.io/scalar-labs/scalardl-schema-loader:<version> \
    --config database.properties [<SOME_OPTIONS> [, ...]]
  ```