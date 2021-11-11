# Scalar DL Backup Creation and Restoration

Since Scalar DL provides transaction capability on top of non-transactional (possibly transactional) databases non-invasively, you need to take special care of backing up and restoring the databases in a transactionally-consistent way.
This guide shows you how to create and restore transactionally-consistent Scalar DL backups.

## How to create transactionally consistent backup 

Scalar DL service is integrated with the admin interface, which allows you to pause the Scalar DL services using the scalar-admin client tool.

### PAUSE

The `PAUSE` command helps you to create a transactionally consistent backup for non-transactional databases. It will pause the ledger.

```console
docker run -it --rm ghcr.io/scalar-labs/scalar-admin:<version> -c PAUSE -s <SRV_Service_URL>
```

### UNPAUSE

The `UNPAUSE` command helps you start the ledger after creating the backup.

```console
docker run -it --rm ghcr.io/scalar-labs/scalar-admin:<version> -c UNPAUSE -s <SRV_Service_URL>
```


## Create Backup

### Cassandra

Since Cassandra has a built-in replication mechanism, we don't always need a transactionally-consistent backup to recover a Cassandra cluster.

For example, if replication is properly set to 3 and only the data of one of the nodes in a cluster is lost, we don't need a transactionally-consistent backup because the node can be recovered with a normal (transactionally-inconsistent) snapshot and the repair mechanism.
However, if the quorum of nodes of a cluster loses their data, we need a transactionally-consistent backup to restore the cluster to a certain transactionally-consistent point.

To minimize mistakes when doing backup operations, it is recommended to use [Cassy](https://github.com/scalar-labs/cassy).

Cassy is integrated with `scalar-admin`, so it pauses the scalar DL service to create a transactionally-consistent cluster-wide backup.
Please see [the doc](https://github.com/scalar-labs/cassy/blob/master/docs/getting-started.md#take-cluster-wide-consistent-backups) for more details.

### JDBC

You can take a backup with your favorite way for JDBC databases.
One requirement for backup in Scalar DL on JDBC databases is that backups for all the Scalar DL managed tables (including the coordinator table) need to be transactionally-consistent or automatically recoverable to a transactionally-consistent state.
That means that you need to create a consistent snapshot by dumping all tables in a single transaction. For example, you can use `mysqldump` command with `--single-transaction` option in MySQL and `pg_dump` command in PostgreSQL to achieve that.

### Cosmos DB

Cosmos DB continuous mode backup creates Point In Time Restore (PITR), it is recommended to use in production environment.

The easiest way to take a transactionally-consistent backup for Scalar DL on Cosmos DB is to pause the Scalar DL service using the [How to create transactionally consistent backup](#how-to-create-transactionally-consistent-backup) steps. You can use the middle value of the pause time as a restore point.

### DynamoDB

ScalarDL schema loader enables Point-in-time recovery (PITR) for each table in a DynamoDB.

The easiest way to take a transactionally-consistent backup for Scalar DL on DynamoDB is to pause the Scalar DL service using the [How to create transactionally consistent backup](#how-to-create-transactionally-consistent-backup) steps. You can use the middle value of the pause time as a restore point.

## Restore

### Cassandra

To minimize mistakes when doing restore operations, it is recommended to use [Cassy](https://github.com/scalar-labs/cassy).

### JDBC

You can restore the backup with your favorite way for JDBC databases.
you can use `mysql` command in MySQL and `psql` command in PostgreSQL to achieve that. Or when you use Amazon RDS (Relational Database Service) or Azure Database for MySQL/PostgreSQL, 
you can restore to any point within the backup retention period with the automated backup feature.

### Cosmos DB

You can restore the backup based on the [azure official guide](https://docs.microsoft.com/en-us/azure/cosmos-db/restore-account-continuous-backup#restore-account-portal) and you must change the default consistency to STRONG after restoring the data.

### DynamoDB

You can restore the tables one by one using the following steps from the [Amazon DynamoDB console](https://console.aws.amazon.com/dynamodbv2/home),

    A. Restore the PITR (Point-in-time recovery) backup of tables except `scalardb.metadata` on the basis of [AWS official guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/PointInTimeRecovery.Tutorial.html#restoretabletopointintime_console).
        
    B. Create the backup of previously restored tables (A) using the [AWS official guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Backup.Tutorial.html#backup_console).
        
    C. Delete all tables (A) except `scalardb.metadata` table and previously restored tables (B).
        
    D. Restore the previously created backup (B) using the actual table name (previously deleted tables (C)) on the basis of [AWS official guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Restore.Tutorial.html#restoretable_console).

You must enable continuous backup and auto-scaling using the scalardl schema loader or Amazon DynamoDB console. The schema tool doesn't remake the existing tables.
