# Getting Started with Scalar DL

This document explains how to get started with Scalar DL by running your first simple contract with the client SDK.
Here, we assume that you already have a certificate and a private key required to run contracts.

## What is Scalar DL?

Scalar DL is a blockchain-inspired distributed ledger platform, which achieves:

* High tamper-evidence of data
* High scalablility and availablity
* Linearizable consistency and ACID

It manages data as a set of assets, where an asset is composed of a history of data identified by a key called `asset_id` and a historical version number called `age`.
In this document, you will create a very simple application to manage an asset's status.

## Configure properties

The first thing you will need to do is to configure some properties.
The following sample properties are the minimum required to interact with the Scalar DL network.
Please update the values depending on your environment.
```
[client.properties]
# A host name of Scalar DL network server.
scalar.dl.client.server.host=localhost

# An ID of a certificate holder. It must be configured for each private key and unique in the system.
scalar.dl.client.cert_holder_id=foo

# A certificate file path to use.
scalar.dl.client.cert_path=/path/to/foo.pem

# A private key file path to use. 
scalar.dl.client.private_key_path=/path/to/foo-key.pem
```

## Register the certificate

Next, let's register your certificate in the Scalar DL network.
The registered certificate will allow you to register and execute contracts, and will also be used for tamper detection of the data stored in the network.

This time, let's use a simple tool to register your certificate as follows.

```
$ client/bin/register-cert --properties client.properties
```

Note that certificate registration needs a privileged access to the network in production environment since the port is not open to the outside of the network.

## Create a contract

Contracts in Scalar DL are simply Java classes which extend the [`Contract`](https://scalar-labs.github.io/scalardl/javadoc/ledger/com/scalar/dl/ledger/contract/Contract.html) class and override the `invoke` method. Let's take a closer look at the `StateUpdater.java` contract which creates an asset and associates some state with it.

```java
package com.org1.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

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

This contract will extract a client-defined asset ID (`asset_id`) and state (`state`) from the argument and associate the asset ID with the state in the ledger, if the given state is different from the asset's current state.

Next we need to compile the contract. This can be done with:
```
$ ./gradlew assemble
```

This will generate `build/classes/java/main/com/org1/contract/StateUpdater.class`.

## Register the contract

Let's register your contract. Here again we use a simple tool.

```
$ client/bin/register-contract --properties client.properties --contract-id StateUpdater --contract-binary-name com.org1.contract.StateUpdater --contract-class-file build/classes/java/main/com/org1/contract/StateUpdater.class
```

Please set a globally unique ID for the contract ID (e.g. `StateUpdater` in the above command).
You can set different contract IDs on the same contract to clarify "who did what" in a tamper-evident way.
For example, let's think about a voting application.
In the application, anyone can vote with the same voting logic, and hence can use the same Contract, but A's vote and B's vote need to be properly and securely distinguished; A cannot vote for B, and vice versa. Having different contract IDs on the same contract can be utilized to achieve such things.

## Execute the contract

Now you are ready to execute the contract with the following command.

```
$ client/bin/execute-contract --properties client.properties --contract-id StateUpdater --contract-argument '{"asset_id":"some_asset", "state":3}'
```

In the contract argument, the value specified with the key `asset_id` must be unique globally for each asset.
In Scalar DL 1.0, `asset_id` is not a reserved json key name and you can use any json key name, but this might be changed in future versions.

## Create your own contracts

As we explained above, what you need to create your own contracts is extend the `Contract` class and override the `invoke` method as you like.
We are preparing more sample contracts, so please wait for an update.

To quickly run and test your contrats in your local environment, [Scalar DL Emulator](https://github.com/scalar-labs/scalardl-tools/tree/master/emulator) might be useful. It uses a mutable in-memory ledger instead of an immutable ledger database and provides an interactive interface, making it easy to do trial and error testing.

## Interact with ClientService 

The tools we have used above are useful for simple testing purposes, but should not be used for production applications. The Client SDK also provides a service layer called [`ClientService`](https://scalar-labs.github.io/scalardl/javadoc/client/com/scalar/dl/client/service/ClientService.html) which should be used for production applications.
The following is a code snippet showing how to use `ClientService` to execute a contract.

```java
  Injector injector = Guice.createInjector(new ClientModule(new ClientConfig(new File(properties))));
 
  try (ClientService service = injector.getInstance(ClientService.class)) {
    JsonObject jsonArgument = Json.createReader(new StringReader(contractArgument)).readObject();
    ContractExecutionResult result = service.executeContract(contractId, jsonArgument);
    result.getResult().ifPresent(System.out::println);
  } catch (ClientException e) {
    System.err.println(e.getStatusCode());
    System.err.println(e.getMessage());
  }
```

It's pretty self-explanatory. It creates a `ClientService` instance using the dependency injection framework called `Guice`,
and creates some json argument with `javax.json.JsonObject`.
Then, you are ready to execute a contract as shown.
For more information, please take a look at [Javadoc](https://scalar-labs.github.io/scalardl/javadoc/client/).

## References

* [Design document](design.md)
* [Javadoc for client SDK](https://scalar-labs.github.io/scalardl/javadoc/client/)
* [A guide on how to write a good contract](how-to-write-contract.md)
* Javadoc
    * [Client](https://scalar-labs.github.io/scalardl/javadoc/client/)
    * [Ledger](https://scalar-labs.github.io/scalardl/javadoc/ledger/)
