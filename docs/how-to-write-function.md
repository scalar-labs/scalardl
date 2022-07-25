# A Guide on How to Write Function for Scalar DL

This document sets out some guidelines for writing functions for Scalar DL.

## What is a function for Scalar DL ?

A Function (Smart Function) for Scalar DL is a Java program, which extends the predefined base functions such as [JacksonBasedFunction](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/function/JacksonBasedFunction.html) class, written for implementing single business logic. A Function mainly manages the data of a Scalar DL application whereas a Contract manages the evidence of the data. Before looking at this, please check [Getting Started with Scalar DL](getting-started.md) and [How to Write Contract For Scalar DL](how-to-write-contract.md) to understand what Scalar DL is and what Scalar DL can do with contracts.

## Background

Assets managed by Contracts in Scalar DL are tamper-evident and append-only, so their data structure is limited in modeling various applications. Moreover, assets cannot be deleted to guarantee tamper evidence. Many distributed ledger platforms deal with the issue by having another database, such as an RDBMS, in front of a ledger to handle the application's data in the database and write logs to the ledger as evidence. However, this scheme is not ideal since data consistency between the database and the ledger is not always preserved. There could be a case where applications don't have corresponding logs in the ledger due to a catastrophic failure, which defeats the purpose of writing logs to the ledger as evidence. Scalar DL resolves the issue with a different approach by introducing Functions to manage applications' data and making Contracts and Functions execute atomically by utilizing underlying distributed ACID transactions with [Scalar DB](https://github.com/scalar-labs/scalardb).

## Write a Function

Let's take a closer look at `Payment` Function to better understand how to write a function.

```java
public class Payment extends JacksonBasedFunction {
  private final String FROM_KEY_NAME = "from";
  private final String TO_KEY_NAME = "to";
  private final String AMOUNT_KEY_NAME = "amount";
  private final String NAMESPACE_KEY_NAME = "namespace";
  private final String TABLE_KEY_NAME = "table";

  @Nullable
  @Override
  public JsonNode invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonNode functionArgument,
      JsonNode contractArgument,
      @Nullable JsonNode contractProperties) {
    // error handling is omitted
    String fromId = contractArgument.get(FROM_KEY_NAME).asText();
    String toId = contractArgument.get(TO_KEY_NAME).asText();
    int amount = contractArgument.get(AMOUNT_KEY_NAME).asInt();
    String namespace = contractProperties.get(NAMESPACE_KEY_NAME).asText();
    String table = contractProperties.get(TABLE_KEY_NAME).asText();

    Key fromKey = Key.ofText("id", fromId);
    Key toKey = Key.ofText("id", toId);

    // get the account balances
    Optional<Result> account1 =
        database.get(
            Get.newBuilder().namespace(namespace).table(table).partitionKey(fromKey).build());
    Optional<Result> account2 =
        database.get(
            Get.newBuilder().namespace(namespace).table(table).partitionKey(toKey).build());

    // assumes that both accounts exist, but it should be checked in production code
    long balance1 = account1.get().getInt("balance");
    long balance2 = account2.get().getInt("balance");

    if (balance1 - amount < 0) {
      throw new ContractContextException(
          "The account " + fromId + " does not have enough account balance.");
    }

    // transfer amount
    balance1 -= amount;
    balance2 += amount;

    // update the account balances
    database.put(
        Put.newBuilder()
            .namespace(namespace)
            .table(table)
            .partitionKey(fromKey)
            .bigIntValue("balance", balance1)
            .build());
    database.put(
        Put.newBuilder()
            .namespace(namespace)
            .table(table)
            .partitionKey(toKey)
            .bigIntValue("balance", balance2)
            .build());

    return null;
  }
}
```

It is a money transfer application written with Scalar DB API, where getting specified account balances, transferring a specified amount of money between the two account balances, and updating the balances. Please also read the [Scalar DB docs](https://github.com/scalar-labs/scalardb/blob/master/docs/api-guide.md) for more details about Scalar DB API.

### Base Functions

Similar to predefined base Contracts, Scalar DL also provides predefined base Functions. For example, the above PaymentFunction is based on one of the base Functions called JacksonBasedFunction, which allows you to deal with the Function inputs and output in Jackson's JsonNode format.

As of writing this, we provide four base Functions as shown below; however, using JacksonBasedFunction is recommended to balance development productivity and performance well.

| Base Function Class                                                                                                                                        | Type of Function inputs and output                                                                                 | Library                                         |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------- |
| [JacksonBasedFunction](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/function/JacksonBasedFunction.html) (recommended) | [JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/JsonNode.html) | [Jackson](https://github.com/FasterXML/jackson) |
| [JsonpBasedFunction](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/function/JsonpBasedFunction.html)                   | [JsonObject](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonObject.html)                 | [JSONP](https://javaee.github.io/jsonp/)        |
| [StringBasedFunction](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/function/StringBasedFunction.html)                 | [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)                                          | Java Standard Libraries                         |
| [Function](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/function/Function.html) (deprecated)                          | [JsonObject](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonObject.html)                 | [JSONP](https://javaee.github.io/jsonp/)        |


The old [Function](https://scalar-labs.github.io/scalardl/javadoc/ledger/com/scalar/dl/ledger/function/Function.html) is still available, but it is now deprecated and will be removed in a later major version. So, it is highly recommended to use the above new (non-deprecated) Functions as a base Function.


### About the `invoke` arguments

Similar to a Contract using `Ledger` object to manage assets, a Function uses `Database` object to manage records of the underlying database. Note that `Database` implements [Scalar DB](https://github.com/scalar-labs/scalardb) interface so that you can do the CRUD operations base on [the data model](https://github.com/scalar-labs/scalardb/blob/master/docs/design.md#data-model) of Scalar DB. 

A `functionArgument` is a runtime argument for the Function specified by the requester. The argument is not digitally signed as opposed to the contract argument so that it can be used to pass data that is stored in the database but it might be deleted at some later point for some reason.

`contractArgument` and `contractProperties` are the corresponding contract's argument and properties. See [How to Write a Contract](how-to-write-contract.md) to understand what they are.

### How to use Functions

The Function feature is enabled by default; thus, nothing needs to be configured in Ledger except for the following things. If you want to disable the feature, please set `scalar.dl.ledger.function.enabled` to `false` in the properties of Ledger.

#### Add an application-specific schema

Since Functions can read and write arbitrary records through the Scalar DB CRUD interface, Scalar DL can't define the database schema for the Function by itself. It is the applications' owner's responsibility to define such schema and apply it to the database by themselves or asking system admins to do it depending on who owns and manages the database.

#### Register a Function

You then need to register a Function to Ledger before used like you register a Contract.

```
client/bin/register-function --properties client.properties --function-id test-function --function-binary-name com.example.function.TestFunction --function-class-file /path/to/TestFunction.class
```

#### Execute a Function

You can specify a Function to execute along with a Contract to execute.
For example, you can execute a function as follows with the command-line tool.

```
client/bin/execute-contract --properties client.properties --contract-id test-contract --contract-argument '{...}' --function-id test-function --function-argument '{...}'
```

You can also do it with the [ClientService](https://scalar-labs.github.io/scalardl/javadoc/latest/client/com/scalar/dl/client/service/ClientService.html) as follows.

```java
ContractExecutionResult result = clientService.executeContract(contractId, contractArgument, functionId, functionArgument);
```

Like a Contract, a Function can invoke another Function so multiple Functions (and multiple Contracts) can be grouped together. Scalar DL executes a group of Contracts and Functions in an ACID manner so that they can be done atomically and in a consistent, isolated, and durable manner.

## How to use Contracts and Functions properly

Contracts and Functions should be properly used to make the scheme meaningful. As a basic principle, Contracts should be used to manage data that requires tamper-evidence, and Functions should be used to manage data that can be updated or deleted or that needs a more flexible data model. As a good practice, Functions are used to manage applications' data and Contracts are used to manage the logs of applications' execution as evidence. For example in a payment application, a Function manages the account balances of users and a Contract manages the evidence of payment between the users.

## References

* [Getting Started with Scalar DL](getting-started.md)
* [Scalar DL Design Document](design.md)
* [Javadoc](https://scalar-labs.github.io/scalardl/javadoc/)
