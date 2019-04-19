# A Guide on How to Write a Good Contract for Scalar DL

This document sets out some guidelines for writing contracts for Scalar DL.

## What is a contract for Scalar DL ?

A contract (a.k.a Smart Contract) for Scalar DL is a Java program extending [`Contract`](https://scalar-labs.github.io/scalardl/javadoc/ledger/com/scalar/ledger/contract/Contract.html) class written for implementing a single function of business logic.
A contact and its arguments are digitally-signed with the contract owner's private key and passed to the Scalar DL network. This mechanism allows the contract to be only executed by the owner and it makes it possible for the system to detect malicious activity such as data tampering.
Before taking a look at this, it is recommended to check [Getting Started in Scalar DL](dl-getting-started.md) and [Scalar DL v1 design document](dl-design.md) to understand what Scalar DL is and its basic terminologies.

## Write a simple contract

Let's take a closer look at the `StateUpdater` contract example to better understand how to write a contract.

```java
public class StateUpdater extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    if (!argument.containsKey("asset_id") || !argument.containsKey("state")) {
      // ContractContextException is the only throwable exception in a contract and
      // it should be thrown when a contract faces some non-recoverable error
      throw new ContractContextException("please set asset_id and state in the argument");
    }

    String assetId = argument.getString("asset_id");
    int state = argument.getInt("state");

    Optional<Asset> asset = ledger.get(assetId);

    if (!asset.isPresent() || asset.get().data().getInt("state") != state) {
      ledger.put(assetId, Json.createObjectBuilder().add("state", state).build());
    }

    return null;
  }
}
```

### About the arguments

As shown above, the overridden `invoke` method accepts [`Ledger`](https://scalar-labs.github.io/scalardl/javadoc/ledger/com/scalar/ledger/ledger/Ledger.html) for interacting with the ledger, a [`JsonObject`](https://javaee.github.io/javaee-spec/javadocs/javax/json/JsonObject.html) for  (otherwise the request is treated as if it does not exist.the contract argument, and an optional [`JsonObject`](https://javaee.github.io/javaee-spec/javadocs/javax/json/JsonObject.html) for contract properties.
The `Ledger` manages a set of assets. In order to interact with the `Ledger`, you can call `get`, `put` and `scan`.
`get` is used to retrieve the latest asset record of the specified asset. `put` is used to append a new asset record to the specified asset. `scan` is used to traverse the specified asset.
Note that you are only allowed to append an asset record to the asset ledger with this abstraction. Thus, it is always a good thing to design your data with the abstraction before writing a contract for Scalar DL.

`JsonObject` for contract argument is an immutable json object and a runtime argument for the contract specified by the requester.
  can be used to define runtime variables. For example in a banking application, you may have a Payment contract where a payer and a payee are passed to the contract as the argument every time it is executed.

`JsonObject` for contract properties is static variables for the contract. It can be used to define contract's per-instance static variables. 
For example in an agreement application, the business logic for the agreement can be defined as a general contract but the agreement conditions may vary depending on the actual application. The optional properties field allows you to define the agreement conditions such as quorum for each contract instance without hard-coding it in the contract.

#### Variable names in the argument
The variable names of the argument `JsonObject` can be arbitrarily defined in Scalar DL 1.0, so that you can use `asset_id` or `id` or even something else to express asset ID. However, in the later versions, some variables such as `asset_id` and `asset_ids` will be reserved to express references to assets.

#### Grouping assets
The value of `asset_id` can be arbitrarily defined but it is a good practice to have some rules when you want to group assets.
For example, if you want to group them in a certain generation, you can append some generation number to the assets like `{asset_id}-0`.
Or you can group them per organization by having some organization ID as a prefix like `{org-id}-{asset_id}`.


### About the internal

Let's look at the internal of the `invoke` method of `StateUpdater` contract.
It first needs to check if the argument has proper variables and matches with an application context, and
throw `ContractContextException` if they are not properly defined.
`ContractContextException` is the only throwable exception from a contract, and it is used to let the system know not to retry the contract execution because requirements are not fully satisfied.

Then the contract retrieves the `asset_id` and `state` given from the requester, and retrieves `asset` from the ledger with the specified `asset_id`. 
And it updates the asset's state if the asset doesn't exist or the asset's state is different from the current state.
A contract might face some `RuntimeException` when interacting with `Ledger`, but it doesn't need to catch it in the contract. All the exceptions are treated properly by the Scalar DL executor.

This contract will just create or update the state of the specified asset, so it doesn't need to return anything to the requester. So in this case, it can return `null`.
If you want to return something to a requester, you can return an arbitrary `JsonObject`.

### Determinism

One very important thing to note when you write a contract for Scalar DL is that you have to make the contract deterministic.
In other words, a contract must always produce the same output for a given particular input.
The reason why determinism is important is that Scalar DL utilizes this property to detect tampering.
For example, Scalar DL will lazily traverse assets and re-execute contracts to check if there is no discrepancy between the expected outcome and the actual data stored in the ledger.
It also utilizes determinism to make the states of multiple independent Scalar DL components, which are possibly managed by different organizations, the same.

One common way of creating a non-deterministic contract is to generate the time inside the contract and have the output including the ledger states somehow depend on this time.
Such a contract will produce different outputs each time it is executed, and makes the system unable to detect tampering.
If you need to use the time in a contract, you should pass it to the contract as an argument.

## Write a complex contract

If your contract is more than 40 lines of code, it is a good sign that you are probably doing more than one thing with your contract.
It is a good practice to write modularized contracts, where each contract is doing only one thing, and to combine contracts to express more complex business logic.
Here is the example code of doing such nested invocation. 

```java
public class PaymentWithFee extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    if (!argument.containsKey("asset_ids") || !argument.containsKey("amount")) {
      throw new ContractContextException("please set asset_ids and amount in the argument");
    }

    JsonArray array = argument.getJsonArray("asset_ids");
    if (array.size() != 3) {
      throw new ContractContextException("please set asset_ids properly");
    }

    int amount = argument.getInt("amount");
    String toId = array.getString(1);
    String anotherId = array.getString(2);

    String anotherContract = "com.scalar.ledger.contract.payment_example.Payment";
    invoke(anotherContract, ledger, argument);

    // transfer 10% of `amount` from `toId` to `anotherId`
    Asset to = ledger.get(toId).get();
    Asset another = ledger.get(anotherId).get();

    JsonObject toData = to.data();
    int toBalance = toData.getInt("balance");
    JsonObject anotherData = another.data();
    int anotherBalance = anotherData.getInt("balance");

    int fee = amount > 10 ? amount / 10 : 1;
    ledger.put(to.id(), Json.createObjectBuilder(toData).add("balance", toBalance - fee).build());
    ledger.put(another.id(), Json.createObjectBuilder(anotherData).add("balance", anotherBalance + fee).build());

    return null;
  }
}
```

It's to be noted that all the contracts in the nested invocation are executed transactionally (in an ACID manner) in Scalar DL so that they are executed entirely successfully or they are entirely failed.

## Summary

Here are the best practices for writing good contracts for Scalar DL.

* Design your data properly to fit with Ledger abstraction before writing contracts
* Throw `ContractContextException` if a contract faces non-recoverable errors
* Modularize contracts to make each do only one thing, and use nested invocation
* Make contracts deterministic
* Use `asset_id` or `asset_ids` to refer to assets for backward-compatibility 
* Define `asset_id` with some rules when you want to group assets

## References

* [Getting started](getting-started.md)
* [Design document](design.md)
* [Javadoc for client SDK](https://scalar-labs.github.io/scalardl/javadoc/client/)
