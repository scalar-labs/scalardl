# A Guide on How to Backup and Restore Data in Scalar DL

Since Scalar DL uses Scalar DB that provides transaction capability on top of non-transactional (possibly transactional) databases non-invasively,
you need to take special care of backing up and restoring the databases in a transactionally-consistent way.
This guide shows you how to create and restore transactionally-consistent Scalar DL backups.

## Create Backup

### For Transactional Databases

#### JDBC databases

You can take a backup with your favorite way for JDBC databases.
One requirement for backup in Scalar DL on JDBC databases is that backups for all the Scalar DL managed tables need to be transactionally-consistent or automatically recoverable to a transactionally-consistent state.
That means that you need to create a consistent snapshot by dumping all tables in a single transaction. For example, you can use `mysqldump` command with `--single-transaction` option in MySQL and `pg_dump` command in PostgreSQL to achieve that.

### For Non-transactional Databases

#### Basic strategy to create a transactionally-consistent backup

One way to create a transactionally-consistent backup is to take a backup while Scalar DL cluster does not have outstanding transactions.
If an underlying database supports a point-in-time snapshot/backup mechanism, you can take a snapshot during the period.
If an underlying database supports a point-in-time restore/recovery mechanism, you can set a restore point to a specific time (preferably the mid-time) in the period since the system takes backups for each operation in such a case.

To easily achieve this, Scalar DL exposes pause API to make Scalar DL drain outstanding transactions and stop accepting new transactions.
We also provide a simple client program called scalar-admin to make a pause request (and unpause request) to a Scalar DL cluster and obtain a paused duration.

Note that when you use a point-in-time-restore/recovery mechanism, it is recommended to minimize the clock drifts between nodes (Scalar DL nodes and a client node that requests a pause) by using clock synchronization such as NTP.
Otherwise, the time you get as a paused duration might be too different from the time in which the pause was actually conducted, which could restore to a point where ongoing transactions exist.
Also, it is recommended to pause a long enough time (e.g., 10 seconds) and use the mid-time of the paused duration since clock synchronization cannot perfectly synchronize clocks between nodes.

#### Database-specific ways to create a transactionally-consistent backup

**Cassandra**

Cassandra has a built-in replication mechanism, so you do not always have to create a transactionally-consistent backup.

For example, if replication is properly set to 3 and only the data of one of the nodes in a cluster is lost, you do not need a transactionally-consistent backup because the node can be recovered with a normal (transactionally-inconsistent) snapshot and the repair mechanism. 
However, if the quorum of nodes of a cluster loses their data, we need a transactionally-consistent backup to restore the cluster to a certain transactionally-consistent point.

If you want to create a transactionally-consistent cluster-wide backup, please follow [the basic strategy](#general-strategy-to-create-a-transactionally-consistent-backup) section, or 
stop the Cassandra cluster and take the copies of all the nodes of the cluster, and start the cluster. 

To avoid mistakes, it is recommended to use [Cassy](https://github.com/scalar-labs/cassy).
Cassy is also integrated with `scalar-admin` so it can issue a pause request to the application of a Cassandra cluster.
Please see [the doc](https://github.com/scalar-labs/cassy/blob/master/docs/getting-started.md#take-cluster-wide-consistent-backups) for more details.

**Cosmos DB**

You must create a Cosmos DB account with a Continuous backup policy enabled to use point-in-time restore (PITR) feature.
To specify a transactionally-consistent restore point, please pause the Scalar DL service described in the [basic strategy](#basic-strategy-to-create-a-transactionally-consistent-backup).

**DynamoDB**

You must enable the point-in-time recovery (PITR) feature for DynamoDB tables. If you use Scalar DB Schema Loader, it enables PITR by default.
To specify a transactionally-consistent restore point, please pause the Scalar DL service as described in the basic strategy.

## Restore Backup

To restore the backup, you must follow the [Restore Backup](https://github.com/scalar-labs/scalardb/blob/master/docs/backup-restore.md#restore-backup) section.
You must restore Scalar Ledger and Auditor tables with the same restore point if you use Ledger and Auditor.
