# A Guide on How to Use Function for Scalar DL

This document sets out some guidelines for writing functions for Scalar DL.

## What is a function for Scalar DL ?

A Function (Smart Function) for Scalar DL is a Java program extending [`Function`](https://scalar-labs.github.io/scalardl/javadoc/ledger/com/scalar/dl/ledger/function/Function.html) class written for implementing a single function of business logic, which mainly manages the data of an application that is deployed on top of Scalar DL.
Before taking a look at this, it is recommended to check [Getting Started in Scalar DL](dl-getting-started.md) and [Scalar DL v1 design document](dl-design.md) to understand what Scalar DL is and its basic terminologies. Understanding [contact guide](how-to-write-contract.md) is also helpful to understand this guide properly.

## Background

Assets managed by Contracts in Scalar DL are, as most of the DLT platforms including Blockchains, tamper-evident, and append-only so their data structure is limited in modeling wide variety of applications and they are not able to be deleted.
Many of the DLT platforms deal with the issue by having another database such as an RDBMS in front of a ledger to handle the application's data in the database and write logs to the ledger as evidence.
However this scheme is not ideal since data consistency in between the database and the ledger is not always preserved. There could be a case where applications don't have corresponding logs in the ledger due to a catastrophic failure, which defeats the purpose of writing logs to the ledger as evidence.
Scalar DL resolves the issue with a little different approach by introducing Functions to manage applications' data and making Contracts and Functions execute atomically by utilizing underlining distributed ACID transactions.

## Write a Function

Let's take a closer look at `PaymentFunction` to better understand how to write a function.

```java
public class PaymentFunction extends Function {

  @Override
  public void invoke(
      Database database,
      Optional<JsonObject> functionArgument,
      JsonObject contractArgument,
      Optional<JsonObject> contractProperties) {
    JsonArray array = contractArgument.getJsonArray(ASSETS_ATTRIBUTE_NAME);
    int amount = contractArgument.getInt(AMOUNT_ATTRIBUTE_NAME);
    String fromId = array.getString(0);
    String toId = array.getString(1);

    Optional<Result> account1 = database.get(new Get(new Key(new TextValue("id", fromId))));
    Optional<Result> account2 = database.get(new Get(new Key(new TextValue("id", toId))));

    long balance1 = ((BigIntValue) account1.get().getValue("balance").get()).get();
    long balance2 = ((BigIntValue) account2.get().getValue("balance").get()).get();

    // Transfer
    balance1 -= amount;
    balance2 += amount;

    Put put1 =
        new Put(new Key(new TextValue("id", fromId)))
            .withValue(new BigIntValue("balance", balance1));
    Put put2 =
        new Put(new Key(new TextValue("id", toId)))
            .withValue(new BigIntValue("balance", balance2));

    database.put(put1);
    database.put(put2);
  }
}
```

As you can easily see, it is a money transfer application, which sends the specified amount of moneny in between two accounts.

### About the arguments

Similar to a Contract using `Ledger` object to manage assets, a Function uses `Database` object to manage records in the underlining database. Note that `Database` implements [Scalar DB](https://github.com/scalar-labs/scalardb) interface so that you can do CRUD operations base on [the data model](https://github.com/scalar-labs/scalardb/blob/master/docs/design.md#data-model) of Scalar DB. 

`JsonObject` for a function argument is a json object and a runtime argument for the function specified by the requester. The argument is not digitally signed as opposed to the contract argument so that it can be used to pass data that is stored in the database but it might be deleted at some later point for some reason.

`contractArgument` and `contractProperties` are the corresponding contract's argument and properties. See [the contract guide](how-to-write-contract.md) to understand what they are.

### How Functions and Contracts are tied together

Functions to be executed are specified in a contract argument with `__functions__` json key. As described in [the contract guide](how-to-write-contract.md#write-a-complex-contract), a contract can invoke another contract, so multiple contracts and multiple functions can be grouped together.

Scalar DL executes the group of opeations in a ACID manner so that they can be done atomically and in a consistent, isolated and durable manner.

### How to add an application-specific schema

Since Functions can read and write arbitrary records through the Scalar DB CRUD interface, Scalar DL can't define the database schema for the Function by itself.
It is applications' onwer's responsibility to define such schema and apply it to the database by themselves or asking system admins to do it depending on who owns and manages the database.

## How to use Contracts and Functions properly

Contracts and Functions should be properly used to make the scheme meaningful.
As a basic principle, Contracts should be used to manage data that requires tamper-evidence, and Functions should be used to manage data that can be updated or deleted or that needs more flexible data model.
As a good practice, Functions are used to manage applications' data and Contracts are used to manage the logs of applications' execution as evidence. For example in a payment application, a Function manages the account balances of users and a Contract manages the evidence of payment in between the users.

## References

* [Getting started](getting-started.md)
* [Design document](design.md)
* [Javadoc for Java client SDK](https://scalar-labs.github.io/scalardl/javadoc/client/)
