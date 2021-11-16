# Scalar DL Backup Creation and Restoration

Since Scalar DL provides transaction capability on top of non-transactional (possibly transactional) databases non-invasively, you need to take special care of backing up and restoring the databases in a transactionally-consistent way.
This guide shows you how to create and restore transactionally-consistent Scalar DL backups.

## How to create transactionally consistent backup 

Scalar DL service is integrated with the admin interface, which allows you to pause the Scalar DL services using the scalar-admin client tool.
You can use the scalar-admin client tool as a docker container or  fat jar, you can find scalar-admin-<version>-all.jar in the [releases](https://github.com/scalar-labs/scalar-admin/releases).

### Requirements

* You must wait at least 10 sec after pausing to create a backup.
* You must identify a common restore point for the Ledger and Auditor if you use Auditor.
### Pause

The `PAUSE` command helps you to create a transactionally consistent backup for non-transactional databases. It helps to pause the Scalar DL service.

```console
java -jar scalar-admin-<version>-all.jar -c PAUSE -s <SRV_Service_URL>

OR

docker run -it --rm ghcr.io/scalar-labs/scalar-admin:<version> -c PAUSE -s <SRV_Service_URL>
```

### Unpause

The `UNPAUSE` command helps you start the ledger after creating the backup.

```console
java -jar scalar-admin-<version>-all.jar -c UNPAUSE -s <SRV_Service_URL>

OR

docker run -it --rm ghcr.io/scalar-labs/scalar-admin:<version> -c UNPAUSE -s <SRV_Service_URL>
```


## Create Backup

This section shows how to create a transactionally-consistent backup for Scalar DL.

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

You should create Cosmos DB account using `Continuous` backup policy to create continuous backups.

Follow [How to create transactionally consistent backup](#how-to-create-transactionally-consistent-backup) to create a transactionally-consistent backup.

### DynamoDB

You should create the schema using the scalardl schema loader, which enables Point-in-time recovery (PITR) for each table in a DynamoDB by default.

Follow [How to create transactionally consistent backup](#how-to-create-transactionally-consistent-backup) to create a transactionally-consistent backup.

## Restore

This section shows how to restore transactionally-consistent backup for Scalar DL.

### Requirements

* You must use the midtime of the pause as a restore point.
* You must restore Scalar Ledger and Auditor tables with the same restore point if you use Ledger and Auditor.

### Cassandra

To minimize mistakes when doing restore operations, it is recommended to use [Cassy](https://github.com/scalar-labs/cassy).

### JDBC

You can restore the backup with your favorite way for JDBC databases.
You can use `mysql` command in MySQL and `psql` command in PostgreSQL to achieve that. Or when you use Amazon RDS (Relational Database Service) or Azure Database for MySQL/PostgreSQL, 
you can restore to any point within the backup retention period with the automated backup feature.

### Cosmos DB

You can restore the backup based on the [azure official guide](https://docs.microsoft.com/en-us/azure/cosmos-db/restore-account-continuous-backup#restore-account-portal) and you must change the default consistency to STRONG after restoring the data.

### DynamoDB

You can restore the tables one by one using the following steps from the [Amazon DynamoDB console](https://console.aws.amazon.com/dynamodbv2/home),

* Restore with PITR of table A to another table B (alias table)
* Take a backup of the restored table B (say, backup B)
* Remove table A
* Create a table named A with backup B

You must enable continuous backup and auto-scaling using the scalardl schema loader or Amazon DynamoDB console. The schema tool doesn't remake the existing tables.
