# Trouble-shooting Guilde

- [Introduction](#introduction)
- [DB](#DB)
- [DL](#DL)
- [Doubt of inconsistency](#doubt-of-inconsistency)

# Introduction
In this document, we will explain what you should do when you have trouble with system development, operation, or maintenance. We will explain, not only how to troubleshoot ScalarDB and DL but also all other components. For example, you receive an error from a high-level component with the message, "update failed with the wrong values and request ScalarDB or DL to update", and you may feel that ScalarDL or DB has a bug but it could just mean an invalid request was sent.

In the first section, we will explain about ScalarDB exceptions and unknown transaction state of a transaction. The second section is concerned with ScalarDL exceptions. Finally, in the last section, we will explain how to investigate inconsistency.

# ScalarDB
## Storage Exceptions

### ConnectionException
This exception is thrown when your application can’t connect to the storage system, like Cassandra. You can resolve this issue by checking the storage status and configurations, ScalarDB configurations. For example, this is thrown in the following cases.

- Failure to open port 9042 on a Cassandra node
- Failure to specify a nodes address for scalar.database.contact_points in properties

### ExecutionException
This exception is very common. It is thrown when a request fails. There are too many causes of this exception to be too specific. In the first case, please read the message in the exception.

This exception doesn’t always mean a fatal error. For instance, there is a case where a request in Cassandra has failed due to network delay or load. In this case, the request may be executed if you retry. If this failure happens frequently because the load on the storage node is high, you should consider upgrading the node.

### InvalidUsageException
This exception indicates that there is an error in the corresponding request. You will need to fix the request according to the usage of the API. For example, this exception is thrown when you request multiple records using a `get()` request. In this case, you should have used a `scan()`.

### MultiPartitionException
This is a subclass of `InvalidUsageException` when a `batch` operation is used. As the message of this exception says, a multi-partition batch isn't recommended. You may have to change the schema of the storage or issue requests for each record.

### NoMutationException
This is a subclass of `ExecutionException`. It is thrown when the corresponding record of a request hasn't met the condition of the request. For example, this is thrown when you try to put a record with `PutIfNotExists()` condition and the record with the same key exists.

### ReadRepairableExecutionException
This is also a subclass of `ExecutionException`. The request which has failed with this exception may have actually succeeded. This means that you do not know whether the corresponding record has been inserted, updated, or not. You will want to check the current state of the record by issuing another read request.

### RetriableExecutionException
This is also a subclass of `ExecutionException`. It is thrown when the storage failed to write a record, but the cause is temporary, not fatal. It may be executed successfully if you retry. If this exception happens frequently, it may help to check the load on the storage.

### StorageRuntimeException
This exception is thrown when the specified keyspace or table does not exist. You can try to check if the keyspace or table actually exists, that the request specifies the correct name, or that the instance of `StorageService` exists.

### UnsupportedTypeException
This exception is thrown when you use an unsupported type. See the [supported types](https://scalar-labs.github.io/scalardb/javadoc/com/scalar/database/io/package-summary.html).

## Exceptions about transaction
### CommitConflictException
This is a subclass of `CommitException`. It is thrown when a transaction fails to commit its updates because another transaction is going to commit updates for the same records. All updates have been aborted. They will be executed successfully if you request a new transaction which has the same operations.

### CommitException
This exception is thrown when a transaction fails to commit its updates. There are many causes of this failure. It is important to note that no records will have been committed by the failed transaction. In many cases, the transaction can be executed successfully if requested again. If this exception happens frequently, you may want to check the load on the storage.

### CoordinatorException
Basically, this exception is wrapped by a `CommitException`. It is internally thrown when an operation to the coordinator fails. When you directly access the coordinator, you will need to handle this exception.

### CrudException
This exception is a basic one. It is thrown when a transaction fails to get records. This failure is caused by getting a record which another transaction has tried to commit, or by a read failure from the storage. You may retry any reads with a new transaction because the all updates of the failed transaction have been aborted.

### CrudRuntimeException
This is thrown when a transaction tries to get a record which has been updated in the same transaction. Updating data, then reading that data is not allowed in the same transaction.

### InvalidUsageException
This exception is thrown when the usage of a transaction is wrong. For example, a blind delete, which is deleting a record without first reading the record, will cause this exception.

### RequiredValueMissingException
For now, this exception isn't thrown by a transaction. If you want to access the coordinator and instantiate `Coordinator.State`, you might see this exception. The coordinator table might not have a column. Please check the schema of the coordinator.

### TransactionException
This is the superclass of exceptions that handle a transaction. The exceptions which extend `TransactionException` do not always mean fatal failures. In many cases, retrying the requests with a new transaction may lead to it being executed successfully.

### TransactionRuntimeException
This is the superclass of exceptions which are caused by fatal operations such as `CrudRuntimeException`. This exception means the request has a bug.

### UncommittedRecordException
This is a subclass of `CrudException`. It is thrown when a transaction tries to get a record which another transaction tries to commit. Like other many exceptions, you can retry the read with a new transaction.

### UnknownTransactionStatusException
This is a subclass of `TransactionException` which is thrown when a transaction fails to commit. There is no way to tell if the transaction has committed or aborted. However, you can check its state by checking the coordinator. This exception is rarely thrown, but it is rather important, so we will explain this unknown state and how to check it in detail in the next section.


## Unknown transaction state
As we mentioned above `UnknownTransactionStatusException` is thrown when a transaction fails to commit and it is not known whether the updates of the transaction are committed or aborted. In this case, you can determine whether the updates of the transaction are committed or not by checking the coordinator.

There are two possibilities in the transaction state stored in the coordinator. They are `COMMITTED` and `ABORTED`. A `COMMITTED` state means that other transactions can get all updates by the transaction. An `ABORTED` state means all updates are gone.

`UnknownTransactionStatusException` happens when the transaction tried to insert its state record to the coordinator, but got some exceptions during the processing and couldn't figure out if the transaction succeeded or not. In this case, the storage may or may not have inserted the record. This insertion to the coordinator will eventually complete by the lazy recovery process in ScalarDB. Therefore, you can check the state which shows whether the transaction has committed or aborted in the following way.

1. Get the transaction ID from this exception by `UnknownTransactionStatusException#getUnknownTransactionId()`
2. Get a state record which is specified with the ID from the coordinator
    - If the state record doesn't exist in the coordinator, retry after reading a record which was updated by the transaction with a new transaction. This reading will initiate the lazy recovery internally.
3. The transaction has been committed if the state is `TransactionState.COMMITTED`. If the state is `TransactionState.ABORTED`, the transaction has been aborted.

The example code to check the coordinator is `TransactionUtility#checkCoordinatorWithRetry()`.


# ScalarDL
## Exceptions

### AssetbaseException
This is a subclass of `DatabaseException` and also the superclass of some exceptions. This is thrown when a contract has both `put()` and `scan()`. In ScalarDL, `scan()` is only for read-only contracts.

### AssetbaseIOException
This is a subclass of `AssetbaseException`. This is thrown when reading assets from the ledger (storage) fails. It is similar to `CrudException` in ScalarDB. If you retry to execute the contract, it will be executed successfully. When it fails repeatedly, you will need to check the storage or configurations.

### AssetCommitException
This is a subclass of `AssetbaseException`. This is thrown when a contract(transaction) fails to be committed like `CommitException`. This is caused by various situations. The contract hasn't updated any asset at all. You can retry to execute the contract.

### AssetOverwriteException
This is a subclass of `AssetCommitException` and is wrapped as `AssetCommitException` internally. This is thrown when a contract failed because another contract tried to be committed for the same asset.

### ContractContextException
This is a subclass of `ContractException`. This is the only exception that your contracts can throw only when your contracts can not meet applications' contexts or requirements. For example, some Pay contract which transfers some funds in a payment application, this exception should be thrown in cases such as lack of balance of payee. When the system catches this exception, it will abort the contract execution and will not retry because it is not recoverable by itself.

### ContractException
This is a subclass of `LedgerException` and the superclass of some exceptions about your contract's context.

### ContractExecutionException
This is a subclass of `ContractException`. This is thrown when assets being updated weren't specified in the argument of a contract when the asset-proof feature is enabled.

### ContractValidationException
This is a subclass of `ValidationException`. This is thrown when a request to register a contract doesn't have the correct signature for the contract. A contract should be registered with the correct signature. You have to check the signature in the request.

### DatabaseException
This is the superclass of some exceptions such as `AssetbaseException`.

### KeyException
This is the superclass of exceptions such as `UnloadableKeyException`.

### LedgerException
This is the superclass of exceptions such as `ContractException`. This is mainly thrown when asset-proof fails. These failures happen when `LedgerConfig.ASSET_PROOF_ENABLED` is `true`. You have to read the message in the exception and resolve it.

### MissingCertificateException
This is a subclass of `RegistryException`. This is thrown when the specified certificate doesn't exist in the storage. You need to review the holder ID and the version of the certificate, or you have to register the corresponding certificate.

### MissingContractException
This is a subclass of `RegistryException`. This is thrown when the specified contract doesn't exist in the storage. You need to review the holder ID, the version of the certificate and the contract ID, or you have to register the corresponding contract.

### RegistryException
This is a subclass of `DatabaseException` and the super class of some exceptions like `RegistryIOException`. This is thrown when a registration fails because the contract or the certificate has already been registered.

### RegistryIOException
This is a subclass of `RegistryException`. This is thrown when putting or getting a certificate or a contract into/from storage fails. You will register or get them if you retry. When you can't, you have to check the storage and configurations.

### SecurityException
This is a subclass of `LedgerException` and the super class of `KeyException` and `SignatureException`.

### SignatureException
This is a subclass of `SecurityException`. This is thrown when signing fails or when the signature object is not initialized properly. You need to review how to sign a request.

### UnknownAssetStatusException
The situation in which this exception is thrown in a similar to that of `UnknownTransactionStatusException` from ScalarDB. Please check the documentation of `UnknownTransactionStatusException` for further details.

### UnloadableContractException
This is a subclass of `ContractException`. This is thrown when a contract fails to be loaded. You need to check if the contract has been registered or if the certificate ID, the version and the contract ID are correct.

### UnloadableKeyException
This is a subclass of `KeyException`. This is thrown when validation fails in getting a validator from a certificate.

### ValidationException
This is a subclass of `LedgerException`. This is thrown when the certificate ID and version of a request don't equal to those of a contract. This is also thrown when the server-side hash might be tampered by comparing it with the client-side hash if asset-proof feature is enabled. You have to check the certificate ID and version or have to validate the ledger by issuing a validation request.

# Doubt of inconsistency
## What will be explained?
In this section, we will explain a procedure about how to check for data consistency in ScalarDB/DL. We will generalize the procedure as much as possible to cover common use cases. Please adjust the procedure when needed, to fit your use case.

This chapter focuses on consistency of database transactions from ACID perspective, and is not talking about consistency (a.k.a mutual consistency) between replicas.

In ScalarDB, it's always recommended to use transaction when your application cares about consistency. Because when you use ScalarDB storage, it's your responsibility to manage the consistency of your application.

In ScalarDL, invocation of contracts in one contract execution request are treated as a transaction. In other words, all operations to assets (`asset` is a unit of data operation like `get` or `put`) in one contract execution request are executed in a transaction. If a contract invokes other contracts internally, all of the operations of the outer and the inner contracts are executed in a transaction.

## Check the specifications of your application
First of all, it is always good to understand the specifications of your application properly because what you expect is given from the specifications. In general, many issues are caused by misunderstanding of specifications or hidden specifications. So, specifications should be shared among not only developers and testers, but also business members. Otherwise, you might not get what you expect and mistakenly report inconsistencies.

For example, when transferring funds between two accounts in a payment application, a fee might be paid to another account in certain cases. If you don't know about it, you would think that some funds have disappeared and that data consistency is violated.

## Find the wrong transaction
It can take a long time to find a wrong transaction because you need to know the expected data and compare it with the actual data. To get the expected data you can look at the logs, but sometimes the logs are too big and it is difficult to find.

There are a few easy cases to get it. For example, there is a case where the previous update has been executed properly, but the last update is wrong. However, in these cases, you have to find the wrong update just after the updating has been executed. This isn't realistic. In general, after many updates, you have to find the last update which has been executed properly and you have to find also an update which the data has become wrong.

Unfortunately, in many cases, you can only trace updates one by one. You have lots of logs including logs of your applications, logs of ScalarDB/DL, and logs of the storage like Cassandra. In general, we recommend you start checking logs of your applications at first because it is easier to read logs of your applications than others.

We show a hint to find the wrong transaction. It is `tx_version` or `age`.

When you use ScalarDB, you can see the actual records in the storage. You can get `tx_version` which shows how many times the record has been updated. When you have a doubt about consistency around T times updating the record according to logs of your application, if T is nearly the current `tx_version`, you figure out that the doubtful transaction was executed recently.

You can get all mutation procedures for each asset with ScalarDL. You can know how many times the asset has been updated by `age` as `tx_version`. A record which is specified `id` and `age` is one of the procedures which has the previous state `input` and the current state `output` which has been updated by a contract(transaction). You can also know which contract has been executed for the asset by `contract_id`. These hints help you to find the corresponding logs in your application.

The following records are examples of parts of the actual records on the storage on a sample transfer application. The asset has been updated 147 times because the latest record has `age` `147`. Also, the record specified by the account `id` `5` and `age` `147` has been inserted by `PaymentWithFee` contract with `argument`. The `input` shows the balances of accounts just before the contract. So, the balance of `output` of the previous record specified by `id` `5` and `age` `146` is the same as the balance of `input` of the latest(`age` `147`) record. The balance decreased from `9228` to `9132` by the last `PaymentWithFee` contract. The contract transfers the specified amount (you can see the amount in the `argument`) from an account (the first `asset_id`) to other two accounts (the second and the last `asset_id`). Because the first `asset_id` is this asset and the amount is `96`, the balance of this asset has decreased.

```
 id | age | input                                                                                                                       | output           | argument                                                                         | contract_id
----+-----+-----------------------------------------------------------------------------------------------------------------------------+------------------+----------------------------------------------------------------------------------+-----------------------------------------------------------
  5 | 147 | {"0":{"age":130,"data":{"balance":11199}},"5":{"age":146,"data":{"balance":9228}},"9":{"age":144,"data":{"balance":10391}}} | {"balance":9132} | {"asset_ids":[5,0,9],"amount":96,"nonce":"561ad3cd-0975-4f79-8869-c1068b266abc"} | com.scalar.ledger.contract.payment_example.PaymentWithFee
  5 | 146 |                                          {"5":{"age":145,"data":{"balance":9284}},"9":{"age":141,"data":{"balance":10344}}} | {"balance":9228} |   {"asset_ids":[5,9],"amount":56,"nonce":"8ac17d71-c8fa-4968-9606-7ee55666a72d"} |        com.scalar.ledger.contract.payment_example.Payment
  5 | 145 |                                           {"2":{"age":140,"data":{"balance":8521}},"5":{"age":144,"data":{"balance":9249}}} | {"balance":9284} |   {"asset_ids":[2,5],"amount":35,"nonce":"7134ade8-df95-47f4-8070-ec322a0ff66b"} |        com.scalar.ledger.contract.payment_example.Payment
```

If the contract has been executed by buying something with `100` amount, the requester might be wrong because the amount of the `argument` is `96`. If the amount of the `argument` is `100` and the latest balance is `9132` in this example, you will wonder if the last contract has transferred the wrong amount. In this case, you will review the implementation of the contract.

You would suspect this as an inconsistency of ScalarDL or that your contracts have bugs if you misunderstand the specifications of your application. There are many causes of inconsistencies, so please make sure your application is working as expected first when you find something suspicious.

Next, we will explain some inconsistent cases caused by ScalarDB or DL.  It might be difficult to understand without an internal knowledge about them, but it's worth looking at.

## What happens if there would be a bug in ScalarDB/DL
There are mainly two inconsistent cases. The first case is when a transaction looks like it succeeded but the value that should be updated by the transaction has not been updated. The second case is when a transaction looks like it failed but the value that should be updated by the transaction has, in fact, been updated. In both cases, there might be another conflicting transaction and it might be causing such issues.

In the first case, after a transaction has read, inserted or updated some records and has committed them successfully, the following transaction can't get the inserted records or can get the values on the records which are the old values before the transaction. For example, on the right side of the below figure, transaction `TX 2` has transferred 100 coins from the balance of A to that of B, but the balance of A hasn't updated. You might see the success log about `TX 2` in logs of your application, but the record hasn't been updated.

<p align="center">
<img src="https://github.com/scalar-labs/scalardl/raw/master/docs/images/inconsistency_committed.png" width="480" />
</p>

If you find this just after this committed transaction, it is easy to identify the transaction. However, the following transactions might update the record since it didn't alert you. In this case, you need to trace logs related to the record one by one.

In the second case, after a transaction has failed, other transactions get the inserted or updated values on the records which the transaction tried to update. For example, on the right side of the below figure, transaction `TX 4` has been aborted, but the balance of B has been updated unexpectedly.

<p align="center">
<img src="https://github.com/scalar-labs/scalardl/raw/master/docs/images/inconsistency_aborted.png" width="480" />
</p>

As the previous case, it is easy to see that just after the transaction. If not, you need to trace logs and find the suspicious transaction.

As stated above, one good way of finding out inconsistency is utilizing operational logs. It might be recommended especially in case of mission critical applications that your application is configured to write some informational logs so that they can be used for later investigation or auditing.

## Prevent inconsistencies
What is the most important thing for you is not troubleshooting the cause of inconsistency but preventing inconsistency from happing. The best way to prevent inconsistency is keeping your system normal.

We have been doing a lot of verification for ScalarDB and DL even under network partition or random crash of processes to find corner case bugs. As of writing this, we don't see any inconsistency even under those severe environments, so we assume there will be a lot less possibility of inconsistency under normal environment. Thus, the most important thing to avoid inconsistency is keeping your system normal as long as possible with proper monitoring, alerting and recovery processes/tools.
