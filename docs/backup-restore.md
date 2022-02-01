# A Guide on How to Backup and Restore Data in Scalar DL

Since Scalar DL uses Scalar DB that provides transaction capability on top of non-transactional (possibly transactional) databases non-invasively,
you need to take special care of backing up and restoring the databases in a transactionally-consistent way.
This guide shows you how to create and restore transactionally-consistent Scalar DL backups.

We first describe how to backup and restore the databases of Scalar DL Ledger. Then, we will describe how the process is extended to cover a case where Auditor is used.

## Create Backups of Ledger Databases

### For Transactional Databases

#### JDBC databases

You can take a backup with your favorite way for JDBC databases.
One requirement for backup in Scalar DL on JDBC databases is that backups for all the Scalar DL managed tables (including the coordinator and scalardb tables) need to be transactionally-consistent or automatically recoverable to a transactionally-consistent state.
That means that you need to create a consistent snapshot by dumping all tables in a single transaction. For example, you can use `mysqldump` command with `--single-transaction` option in MySQL and `pg_dump` command in PostgreSQL to achieve that.
Or when you use Amazon RDS (Relational Database Service) or Azure Database for MySQL/PostgreSQL, you can restore to any point within the backup retention period with the automated backup feature, which satisfies the requirement.

### For Non-transactional Databases

#### Basic strategy to create a transactionally-consistent backup

One way to create a transactionally-consistent backup is to take a backup while Scalar DL cluster does not have outstanding transactions.
If an underlying database supports a point-in-time snapshot/backup mechanism, you can take a snapshot during the period.
If an underlying database supports a point-in-time restore/recovery mechanism, you can set a restore point to a specific time (preferably the mid-time) in the period.

To easily achieve this, Scalar DL exposes pause API to make Scalar DL drain outstanding transactions and stop accepting new transactions.
We also provide a simple client program called [scalar-admin](https://github.com/scalar-labs/scalar-admin) to make a pause request (and unpause request) to a Scalar DL cluster and obtain a paused duration.

Note that when you use a point-in-time-restore/recovery mechanism, it is recommended to minimize the clock drifts between nodes (Scalar DL nodes and a client node that requests a pause) by using clock synchronization such as NTP.
Otherwise, the time you get as a paused duration might be too different from the time in which the pause was actually conducted, which could restore to a point where ongoing transactions exist.
Also, it is recommended to pause a long enough time (e.g., 10 seconds) and use the mid-time of the paused duration since clock synchronization cannot perfectly synchronize clocks between nodes.
If you use Auditor, a transactionally consistent backup for Auditor is also created when the ledger is paused.

#### Database-specific ways to create a transactionally-consistent backup

**Cassandra**

Cassandra has a built-in replication mechanism, so you do not always have to create a transactionally-consistent backup.
For example, if replication is set to 3 and only the data of one of the nodes in a cluster is lost, you do not need a transactionally-consistent backup because the node can be recovered with a normal (transactionally-inconsistent) snapshot and the repair mechanism. 
However, if the quorum of nodes of a cluster loses their data, we need a transactionally-consistent backup to restore the cluster to a certain transactionally-consistent point.

If you want to create a transactionally-consistent cluster-wide backup, please follow [the basic strategy](#basic-strategy-to-create-a-transactionally-consistent-backup) section, or 
stop the Cassandra cluster and take the copies of all the nodes of the cluster, and start the cluster. 

To avoid mistakes, it is recommended to use [Cassy](https://github.com/scalar-labs/cassy).
Cassy is also integrated with [`scalar-admin`](https://github.com/scalar-labs/scalar-admin) so it can issue a pause request to the application of a Cassandra cluster.
Please see [the doc](https://github.com/scalar-labs/cassy/blob/master/docs/getting-started.md#take-cluster-wide-consistent-backups) for more details.

**Cosmos DB**

You must create a Cosmos DB account with a Continuous backup policy enabled to use point-in-time restore (PITR) feature. Backups are created continuously after it is enabled.
To specify a transactionally-consistent restore point, please pause Scalar DL service as described in the [basic strategy](#basic-strategy-to-create-a-transactionally-consistent-backup).

**DynamoDB**

You must enable the point-in-time recovery (PITR) feature for DynamoDB tables. If you use [Scalar DL Schema Loader](https://github.com/scalar-labs/scalardl-schema-loader), it enables PITR by default.
To specify a transactionally-consistent restore point, please pause Scalar DL service as described in the [basic strategy](#basic-strategy-to-create-a-transactionally-consistent-backup).

## Restore Backups of Ledger Databases

To restore backups, you must follow the [Restore Backup](https://github.com/scalar-labs/scalardb/blob/master/docs/backup-restore.md#restore-backup) section.
You must restore Scalar Ledger and Auditor tables with the same restore point if you use Ledger and Auditor.

## Create/Restore Backups of Auditor Databases

When you use Auditor, you also need to take backups of Auditor databases in addition to Ledger databases.
To make the backups of Ledger and Auditor databases consistent, you always need to pause a Ledger cluster regardless of whether you use transactional databases or non-transactional databases for Ledger and Auditor.

Here is the steps to take backups:
1. Pause a Ledger cluster
1. Take backups of Ledger databases (as described above)
1. Take backups of Auditor databases (as described above)
1. Unpause the Ledger cluster

Note that, even if Ledger is paused, Auditor still accepts requests and updates its data (i.e., lock tables), however, the updated data is lazily recovered once Ledger is unpaused.
To reduce the lazy recovery overhead, it is always a good practice to take backups while there are no requests for Scalar DL.
We are planning to provide a more efficient scheme as future work.

When restoring backups, make sure you use the backups that are created in the same pause period. 
