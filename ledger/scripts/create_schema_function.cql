DROP KEYSPACE IF EXISTS test;
CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 };

CREATE TABLE IF NOT EXISTS test.function_test (
    id text,
    balance int,
    tx_committed_at bigint,
    tx_id text,
    tx_prepared_at bigint,
    tx_state int,
    tx_version int,
    before_balance int,
    before_tx_committed_at bigint,
    before_tx_id text,
    before_tx_prepared_at bigint,
    before_tx_state int,
    before_tx_version int,
    PRIMARY KEY (id)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };
