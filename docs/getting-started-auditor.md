# Getting Started with Scalar DL Auditor

This document explains how to get started with Scalar DL Auditor.
Here, we assume that you have already finished reading the following guides and gained some experiences in overall Scalar DL.

* [Getting Started with Scalar DL](getting-started.md)
* [A Guide on How to Write a Good Contract for Scalar DL](how-to-write-contract.md)
* [A Guide on How to Write Function for Scalar DL](how-to-write-function.md)
* [A Guide on How to Use Asset Proofs in Scalar DL](how-to-use-proof.md)

## What is Scalar DL Auditor?

Scalar DL Auditor is a component that manages the identical states of Ledger to help clients to detect Byzantine faults.
Using Auditor brings great benefit from the security perspective but it comes with  extra processing cost,
so please think carefully if you really need it.

To make Byzantine fault detection with auditing work properly,
Ledger and Auditor should be deployed and managed in a different administrative domain.
However, for this getting started guide, we assume they are placed in the same network and managed in the same administrative domain.

## Configure properties

You need to configure Ledger and Auditor to make the detection properly work.

For the properties file for Ledger, you need to configure the following entries:
```
[ledger.properties]
scalar.dl.ledger.proof.enabled=true
scalar.dl.ledger.proof.private_key_path=/path/to/private_key.pem
scalar.dl.ledger.nonce_txid.enabled=true
scalar.dl.ledger.auditor.enabled=true
#scalar.dl.ledger.auditor.cert_holder_id=auditor
#scalar.dl.ledger.auditor.cert_version=1
```

You first need to set `scalar.dl.ledger.auditor.enabled` to true to let Ledger know if it is using Auditor.
Note that you also need to enable `scalar.dl.ledger.proof.enabled` and set a proper private key to `scalar.dl.ledger.proof.private_key_path` since Auditor uses [AssetProof](how-to-use-proof.md) to work.
If they are not properly and consistently configured, Ledger will throw an exception.

Also, Ledger manages an Auditor's certificate, i.e., Auditor needs to register its certificate to Ledger, and uses the certificate to validate a request signed by Auditor's private key to identify the request origin; thus, the certificate holder ID (`scalar.dl.ledger.auditor.cert_holder_id`) and version (`scalar.dl.ledger.auditor.cert_version`) are required. 
By default, Ledger assumes Auditor registers its certificate with a name `auditor` and version `1`.



You also need to configure the properties of Auditor as follows:
```
[auditor.properties]
#scalar.dl.auditor.ledger.cert_holder_id=ledger
#scalar.dl.auditor.ledger.cert_version=1
#scalar.dl.auditor.cert_holder_id=auditor
#scalar.dl.auditor.cert_version=1
scalar.dl.auditor.cert_path=/path/to/auditor.pem
scalar.dl.auditor.private_key_path=/path/to/auditor-key.pem
```

Auditor manages a key pair to sign a request before sending the request to Ledger and validate a request given from Ledger,
so you need set `scalar.dl.auditor.cert_path` and `scalar.dl.auditor.private_key_path`.

As similarly to Ledger, Auditor manages Ledger's certificate, i.e., Ledger registers its certificate to Auditor, and uses the certificate to validate a request signed by Ledger's private key; thus, the certificate holder ID (`scalar.dl.auditor.ledger.cert_holder_id=ledger`) and version (`scalar.dl.auditor.ledger.cert_version`) are required.
By default, Auditor assumes Ledger registers its certificate with a name `ledger` and version `1`.

Other values are optional but they need to be updated depending on an environment.
For example, the default settings assume that Ledger is placed in the same host, and Auditor uses locally installed Cassandra through Scalar DB without configuring the password.
Please check [the configuration file](https://github.com/scalar-labs/scalar/blob/master/auditor/conf/auditor.properties.tmpl) for more detail.


## Start Ledger and Auditor

Please start Ledger and Auditor in your own way.
For example, if you use the built-in command line tools, do as follows:

Ledger:
```
bin/scalar-ledger --properties ledger.properties
```

Auditor:
```
bin/scalar-auditor --properties auditor.properties
```

You can also use the containers that are available in [the GitHub Container Registry](https://github.com/orgs/scalar-labs/packages).
See [saclardl-samples](https://github.com/scalar-labs/scalardl-samples) repo for more details about how to use the containers.

## Register each certificates of Ledger and Auditor

As we explained,
Ledger needs to register its certificate to Auditor, and Auditor needs to register its certificate to Ledger.
This can be done by registering it as a client as follows:

Ledger registers its certificate to Auditor
```
bin/register-cert --properties client.properties.ledger
```

Auditor registers its certificate to Ledger
```
bin/register-cert --properties client.properties.auditor
```

Please configure `client.properties.ledger` and `client.properties.auditor` properly.
(This document assumes you know how to configure client.properties properly.)
Note that `scalar.dl.client.cert_holder_id` has to be properly set to match with the one configured in Ledger and Auditor respectively.

## Register your certificate

You also need to update the properties file of a client before interacting with Scalar DL with Auditor.
Please update the following entries:
```
[client.properties]
scalar.dl.client.auditor.enabled=true
scalar.dl.client.auditor.host=localhost
```

Then, you can register your certificate just like you have been doing usually without Auditor.

```
client/bin/register-cert --properties client.properties
```

Not that this registers the certificate to both Ledger and Auditor.

## Register your contracts

For registering contracts, you can also do as before.

```
client/bin/register-contract --properties client.properties --contract-id StateUpdater --contract-binary-name com.org1.contract.StateUpdater --contract-class-file build/classes/java/main/com/org1/contract/StateUpdater.class
```

Note that this registers the contract to both Ledger and Auditor.

## Execute the contract

Now you are ready to execute the contract with the following command as usual.

```
client/bin/execute-contract --properties client.properties --contract-id StateUpdater --contract-argument '{"asset_id":"some_asset", "state":3}'
```

Note that this triggers a little complex protocol between Ledger and Auditor to make them go to the same states without trusting each other.
During the execution, it may detect inconsistencies between them if there is a tampering in one of the components.

## Validate the states of Ledger and Auditor

You can also always validate the states of Ledger and Auditor to see if they are consistent with the following command as usual:

```
client/bin/validate-ledger --properties client.properties --asset-id="some_asset"
```

By default, it checks the states of Ledger and Auditor non-linearizably; i.e., there might be cases where they look inconsistent temporarily.
To make it linearizable, you can configure the following option and resister `ValidateLedger` contract as follows.

```
[client.properties]
scalar.dl.client.auditor.linearizable_validation.enabled=true
```

```
client/bin/register-contract --properties client.properties --contract-id validate-ledger --contract-binary-name com.scalar.dl.client.contract.ValidateLedger --contract-class-file /path/to/ValdateLedger.class
```

Then, the same command now triggers the linearizable validation. 
Note that it always produces the correct result but comes with a cost since it runs the same protocol as the execution step.

