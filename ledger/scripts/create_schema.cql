DROP KEYSPACE IF EXISTS scalar;
CREATE KEYSPACE IF NOT EXISTS scalar WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 };
CREATE KEYSPACE IF NOT EXISTS coordinator WITH replication = {'class': 'SimpleStrategy','replication_factor': 1 };

CREATE TABLE IF NOT EXISTS scalar.asset (
    id text,
    age int,
    argument text,
    before_argument text,
    before_contract_id text,
    before_hash blob,
    before_input text,
    before_output text,
    before_prev_hash blob,
    before_signature blob,
    before_tx_committed_at bigint,
    before_tx_id text,
    before_tx_prepared_at bigint,
    before_tx_state int,
    before_tx_version int,
    contract_id text,
    hash blob,
    input text,
    output text,
    prev_hash blob,
    signature blob,
    tx_committed_at bigint,
    tx_id text,
    tx_prepared_at bigint,
    tx_state int,
    tx_version int,
    PRIMARY KEY (id, age)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };

CREATE TABLE IF NOT EXISTS scalar.asset_metadata (
    asset_id text,
    latest_age int,
    tx_committed_at bigint,
    tx_id text,
    tx_prepared_at bigint,
    tx_state int,
    tx_version int,
    before_latest_age int,
    before_tx_committed_at bigint,
    before_tx_id text,
    before_tx_prepared_at bigint,
    before_tx_state int,
    before_tx_version int,
    PRIMARY KEY (asset_id)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };

CREATE TABLE IF NOT EXISTS scalar.contract (
    id text,
    cert_holder_id text,
    cert_version int,
    binary_name text,
    properties text,
    registered_at bigint,
    signature blob,
    PRIMARY KEY (cert_holder_id, cert_version, id)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };
CREATE INDEX IF NOT EXISTS ON scalar.contract (id);

CREATE TABLE IF NOT EXISTS scalar.contract_class (
    binary_name text,
    byte_code blob,
    PRIMARY KEY (binary_name)
);

CREATE TABLE IF NOT EXISTS scalar.function (
    id text,
    binary_name text,
    byte_code blob,
    registered_at bigint,
    PRIMARY KEY (id)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };

CREATE TABLE IF NOT EXISTS scalar.certificate (
    holder_id text,
    version int,
    pem text,
    registered_at bigint,
    PRIMARY KEY (holder_id, version)
);

CREATE TABLE IF NOT EXISTS scalar.secret (
    entity_id text,
    key_version int,
    secret_key blob,
    registered_at bigint,
    PRIMARY KEY (entity_id, key_version)
);

CREATE TABLE IF NOT EXISTS scalar.tx_state (
    id text,
    state int,
    created_at bigint,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS scalar.namespace (
    name text,
    PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS coordinator.state (
    tx_id text,
    tx_state int,
    tx_created_at bigint,
    PRIMARY KEY (tx_id)
);
