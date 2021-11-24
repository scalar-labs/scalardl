# How to Backup and Restore Databases Integrated with Scalar DL

Since Scalar DL provides transaction capability on top of non-transactional (possibly transactional) databases non-invasively, you need to take special care of backing up and restoring the databases in a transactionally-consistent way.
This guide shows you how to create and restore transactionally-consistent Scalar DL backups.

## Create Backup

### For Transactional Databases

#### JDBC databases

You can take a backup with your favorite way for JDBC databases.
One requirement for backup in Scalar DL on JDBC databases is that backups for all the Scalar DL managed tables need to be transactionally-consistent or automatically recoverable to a transactionally-consistent state.
That means that you need to create a consistent snapshot by dumping all tables in a single transaction. For example, you can use `mysqldump` command with `--single-transaction` option in MySQL and `pg_dump` command in PostgreSQL to achieve that.

### For Non-transactional Databases

#### General strategy to create a transactionally-consistent backup

Scalar DL service is integrated with the admin interface, which allows you to pause the Scalar DL services using the [scalar-admin](https://github.com/scalar-labs/scalar-admin) client tool.
You can use the scalar-admin client tool as a docker container or fat jar, you can find scalar-admin-<version>-all.jar in the [releases](https://github.com/scalar-labs/scalar-admin/releases).

You must pause for a long enough time (e.g., 10 seconds) to create a backup and use the midtime of the pause as a restore point.

#### Database specific strategies to create a transactionally-consistent backup

##### Cassandra

Cassandra has a built-in replication mechanism, so you do not always have to create a transactionally-consistent backup.

For example, if replication is properly set to 3 and only the data of one of the nodes in a cluster is lost, you do not need a transactionally-consistent backup because the node can be recovered with a normal (transactionally-inconsistent) snapshot and the repair mechanism. 
However, if the quorum of nodes of a cluster loses their data, we need a transactionally-consistent backup to restore the cluster to a certain transactionally-consistent point.

If you want to create a transactionally-consistent cluster-wide backup, please follow the [General strategy to create a transactionally-consistent backup](#general-strategy-to-create-a-transactionally-consistent-backup) section, or 
stop the Cassandra cluster and take the snapshots of all the nodes of the cluster, and start the cluster. 

To minimize mistakes when doing backup operations, it is recommended to use [Cassy](https://github.com/scalar-labs/cassy).
Cassy is also integrated with `scalar-admin` so it can issue a pause request to the application of a Cassandra cluster.
Please see [the doc](https://github.com/scalar-labs/cassy/blob/master/docs/getting-started.md#take-cluster-wide-consistent-backups) for more details.

##### Cosmos DB

You must create a Cosmos DB account with a Continuous backup policy to create point-in-time recovery (PITR). Please follow the [General strategy to create a transactionally-consistent backup](#general-strategy-to-create-a-transactionally-consistent-backup) section to create a backup.

##### DynamoDB

You must create tables with point-in-time recovery (PITR) and autoscaling in DynamoDB, scalardb schema Loader enables PITR and autoscaling by default. Please follow the [General strategy to create a transactionally-consistent backup](#general-strategy-to-create-a-transactionally-consistent-backup) section to create a backup.

## Restore Backup

To restore the backup, you must follow the [Restore Backup](https://github.com/scalar-labs/scalardb/blob/master/docs/backup-restore.md#restore-backup) section.
You must restore Scalar Ledger and Auditor tables with the same restore point if you use Ledger and Auditor.

