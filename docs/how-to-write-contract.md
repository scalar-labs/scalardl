# A Guide on How to Write a Good Contract for Scalar DL

This document sets out some guidelines for writing contracts for Scalar DL.

## What is a contract for Scalar DL ?


A contract (a.k.a Smart Contract) for Scalar DL is a Java program extending predefined base contracts (that also extend [ContractBase](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/contract/ContractBase.html) class) written for implementing single business logic. A contract and its arguments are digitally-signed with the contract owner's private key and passed to the Scalar DL. This mechanism allows the contract only to be executed by the owner and makes it possible for the system to detect malicious activity such as data tampering.

Before looking at this document, please check the [Getting Started with Scalar DL](getting-started.md) to understand what Scalar DL is and its basic terminologies.

## Write a simple contract

Let's take a closer look at the `StateUpdater` contract example to better understand how to write a contract. 

```java
public class StateUpdater extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    if (!argument.has("asset_id") || !argument.has("state")) {
      // ContractContextException is the only throwable exception in a contract and
      // it should be thrown when a contract faces some non-recoverable error
      throw new ContractContextException("please set asset_id and state in the argument");
    }

    String assetId = argument.get("asset_id").asText();
    int state = argument.get("state").asInt();

    Optional<Asset<JsonNode>> asset = ledger.get(assetId);

    if (!asset.isPresent() || asset.get().data().get("state").asInt() != state) {
      ledger.put(assetId, getObjectMapper().createObjectNode().put("state", state));
    }

    return null;
  }
}
```

### Base contracts

The internal representation of the Ledger data and Contract arguments is String. However, dealing with structured data with String is error-prone and not always easy. The base contracts define other easy-to-handle data types for the Ledger data and Contract arguments. They also manage serialization and deserialization between the data types and String.

For example, the above `StateUpdater` contract is based on one of the base contracts called `JacksonBasedContract`, which allows you to deal with the Ledger data and Contract arguments in [Jackson](https://github.com/FasterXML/jackson)'s [JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/JsonNode.html) format.

As of writing this, we provide four base contracts as shown below; however, using `JacksonBasedContract` is recommended to balance development productivity and performance well.

| Base Contract Class                                                                                                                                        | Type of Contract Argument, Contract Properties, Contract Output, and Ledger Data                                   | Library                                         |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------- |
| [JacksonBasedContract](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/contract/JacksonBasedContract.html) (recommended) | [JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/JsonNode.html) | [Jackson](https://github.com/FasterXML/jackson) |
| [JsonpBasedContract](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/contract/JsonpBasedContract.html)                   | [JsonObject](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonObject.html)                 | [JSONP](https://javaee.github.io/jsonp/)        |
| [StringBasedContract](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/contract/StringBasedContract.html)                 | [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)                                          | Java Standard Libraries                         |
| [Contract](https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/contract/Contract.html) (deprecated)                          | [JsonObject](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonObject.html)                 | [JSONP](https://javaee.github.io/jsonp/)        |


The old [Contract](https://scalar-labs.github.io/scalardl/javadoc/ledger/com/scalar/dl/ledger/contract/Contract.html) is still available, but it is now deprecated and will be removed in a later major version. So, it is highly recommended to use the above new (non-deprecated) contracts as a base contract.

### About the `invoke` arguments

As shown above, the overridden `invoke` method accepts [Ledger](
https://scalar-labs.github.io/scalardl/javadoc/latest/ledger/com/scalar/dl/ledger/statemachine/Ledger.html) for interacting with the underlying database, a [JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/JsonNode.html) for the contract argument, and an optional [JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/JsonNode.html) for contract properties.

The `Ledger` is a database abstraction that manages a set of assets, where each asset is composed of the history of a record identified by a key called `asset_id` and a historical version number called `age`.  You can interact with the `Ledger` with `get`, `put`, and `scan` APIs. The `get` API is used to retrieve the latest asset record of a specified asset. The `put` API is used to append a new asset record to a specified asset. The `scan` API is used to traverse a specified asset. Note that you can only append an asset record to the ledger with this abstraction. Thus, it is always a good practice to design your data with the abstraction before writing a contract for Scalar DL.

The contract argument is a runtime argument for the contract specified by the requester. The contract argument is usually used to define runtime variables. For example in a banking application, you may have a Payment contract where a payer and a payee are passed to the contract as the argument every time it is executed.

The contract properties is static variables for the contract. It can be used to define contract's per-instance static variables. 
For example in an agreement application, the business logic for the agreement can be defined as a general contract but the agreement conditions may vary depending on the actual application. The optional properties field allows you to define the agreement conditions such as quorum for each contract instance without hard-coding it in the contract.

### About the `StateUpdater` logic

The `StateUpdater` contract first checks if the argument has proper variables, matches with an application context, and throws `ContractContextException` if they are not adequately defined. `ContractContextException` is the only throwable exception from a contract, and it is used to let the system know not to retry the contract execution because requirements are not fully satisfied.

Then the contract retrieves an `asset_id` and `state` given from the requester and retrieves `asset` from the Ledger with the specified `asset_id`. And it updates the asset's state if the asset doesn't exist or the asset's state is different from the current state.
A contract might face some `RuntimeException` when interacting with the Ledger, but it shouldn't catch it in the contract. All the exceptions are treated properly by the Scalar DL executor.

This contract will just create or update the state of an specified asset, so it doesn't need to return anything to the requester. So in this case, it can return `null`. If you want to return something to a requester, you can return an arbitrary `JsonNode` when using JacksonBasedContract.

### Grouping assets
The value of `asset_id` can be arbitrarily defined but it is a good practice to have some rules when you want to group assets.
For example, if you want to group them in a certain generation, you can append some generation number to the assets like `{asset_id}-0`.
Or you can group them per organization by having some organization ID as a prefix like `{org-id}-{asset_id}`.



### Exception handling

Note that you should not do any exception handling in contracts except for throwing `ContractContextException` as mentioned above.
Thus, `Ledger` might throw some runtime (unchecked) exceptions in case it can not proceed for some reason, but the exceptions should not be caught. Exceptions are handled properly outside of contracts.

### Determinism

One very important thing to note when you write a contract for Scalar DL is that you have to make the contract deterministic. In other words, a contract must always produce the same output for a given particular input. This is because Scalar DL utilizes determinism to detect tampering.

For example, Scalar DL will lazily traverse assets and re-execute contracts to check if there is no discrepancy between the expected outcome and the actual data stored in the ledger. It also utilizes determinism to make the states of multiple independent Scalar DL components (i.e., Ledger and Auditor) the same.

One common way of creating a non-deterministic contract is to generate the time inside the contract and have the output including the ledger states somehow depend on this time. Such a contract will produce different outputs each time it is executed and makes the system unable to detect tampering. If you need to use the time in a contract, you should pass it to the contract as an argument.

### Deleting an asset

The assets registered through contracts are not able to be deleted to provide tamper-evidence. However, there are cases where you want to delete some assets to follow the rules and regulations of applications you develop. To provide such a data deletion, Scalar DL supports a feature called `Function`.

For more details about `Function`, please check [How to Write Function for Scalar DL](./how-to-write-function.md) guide.


## Write a complex contract

If your contract is more than 100 lines of code, it is a good sign that you are probably doing more than one thing with your contract.
It is a good practice to write modularized contracts, where each contract is doing only one thing, and to combine contracts to express more complex business logic.

The following is the example code of doing such nested invocation. Assume that `StateReader`, which reads the state of a specified asset, has been registered with `state-reader` as a contract ID. 

```java
public class StateUpdaterReader extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    if (!argument.has("asset_id") || !argument.has("state")) {
      // ContractContextException is the only throwable exception in a contract and
      // it should be thrown when a contract faces some non-recoverable error
      throw new ContractContextException("please set asset_id and state in the argument");
    }

    String assetId = argument.get("asset_id").asText();
    int state = argument.get("state").asInt();

    Optional<Asset<JsonNode>> asset = ledger.get(assetId);

    if (!asset.isPresent() || asset.get().data().get("state").asInt() != state) {
      ledger.put(assetId, getObjectMapper().createObjectNode().put("state", state));
    }

    return invoke("state-reader", ledger, argument);
  }
}
```

The `StateUpdaterReader` updates the Ledger just like `StateUpdater` and additionally calls another invoke with the `state-reader` to read what was written. Although this example might not be very convincing, but modularizing contracts (e.g., defining `StateUpdater` separately) can make the contracts reusable.

It's to be noted that all the contracts in the nested invocation are executed transactionally (in an ACID manner) in Scalar DL so that they are executed entirely successfully or they are entirely failed.

## Summary

Here are the best practices for writing good contracts for Scalar DL.

* Design your data properly to fit with Ledger abstraction before writing contracts
* Throw `ContractContextException` if a contract faces non-recoverable errors
* Do not do any exception handling except for throwing `ContractContextException`
* Modularize contracts to make each do only one thing, and use nested invocation
* Make contracts deterministic
* Define `asset_id` with some rules when you want to group assets

## More samples

You can find more contract samples in [caliper-benchmarks](https://github.com/scalar-labs/caliper-benchmarks/tree/scalardl/src/scalardl/src/main/java/com/example/contract).


## References

* [Getting Started with Scalar DL](getting-started.md)
* [Scalar DL Design Document](design.md)
* [Javadoc](https://scalar-labs.github.io/scalardl/javadoc/)
