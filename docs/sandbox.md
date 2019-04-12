# Getting Started with Scalar DL Sandbox

This document explains how to get started with Scalar DL Sandbox.

## Purpose of Sandbox

The Sandbox environment is for playing around with Scalar DL to roughly understand what Scalar DL does and how to write contracts.
This is not for verifying or benchmarking the reliability, scalability and/or performance of Scalar DL.
If you want to interact with Scalar DL more deeply, please [contact us](https://scalar-labs.com/contact_us/).

## Get an auth token and a key pair (a certificate and a private key)

You will need a [GitHub](https://github.com/) account to continue.
If you don't have one, please create a free account.

We will authorize you through GitHub OAuth to grant you access to the Sandbox environment.
Please visit [our sandbox site](https://scalar-labs.com/sandbox/), read the [terms of use](https://scalar-labs.com/terms-of-use), and press the button `Try Now`.
We will provide you with a zip file containing the necessary access token, key pair and configuration file.
The access token is only used for authentication with Sandbox API gateway.
The key pair is used for communicating with Scalar DL network.

Please note that we generate a key pair for ease of use for the Sandbox environment, but it is usually required to create your private key in your own environment.

## Before running your first contract 

Please make sure you have the [Oracle JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed, and, if you haven't already, download the Scalar DL Client SDK.

```
$ git clone https://github.com/scalar-labs/scalardl-client-sdk.git 
```

Scalar DL manages data as a set of assets. Each asset is identified by its `asset_id`, an arbitrary, but unique, string specified by a user to manage the asset, and consists of its change history. Each history item is made up of the `asset_id`, an `age` (starting from zero), and `data` at the specified age.

Since the Sandbox is a shared environment that anyone can access,
take special care when choosing appropriate names for your `asset_id`s so that they will not conflict with the `asset_id`s chosen by other user.
One recommended way to do this is to append your username to the asset name, for example `<username>-<your-asset-name>`.
Also, the same care is needed when choosing contract IDs and we recommend using `<username>-<your-contract-class-name>`.

## Register your certificate

Next, let's register your certificate in the Scalar DL network.
The registered certificate will allow you to register and execute contracts, and is also used for tamper detection of the data stored in the network.

In the `scalardl-client-sdk` directory:
```
$ client/bin/register-cert -properties client.properties
```
* The `client.properties` should be the same file from the zip downloaded earlier.

## Run the StateUpdater contract

We will run the contract [`scr/main/java/com/org1/contract/StateUpdater.java`](https://github.com/scalar-labs/scalardl-client-sdk/blob/master/src/main/java/com/org1/contract/StateUpdater.java), which manages status of some asset.

In the `scalardl-client-sdk` directory:

1. Compile the contract

    ```
    $ ./gradlew assemble
    ```

    This will generate `build/classes/java/main/com/org1/contract/StateUpdater.class`.

2. Register the contract

    NOTE: Please replace `<username>` with your GitHub username.

    ```
    $ client/bin/register-contract -properties client.properties -contract-id <username>-StateUpdater -contract-binary-name com.org1.contract.StateUpdater -contract-class-file build/classes/java/main/com/org1/contract/StateUpdater.class
    ```

3. Execute the contract

    NOTE: Please replace `<username>` with your GitHub username.
    ```
    $ client/bin/execute-contract -properties client.properties -contract-id <username>-StateUpdater -contract-argument '{"asset_id": "<username>-myasset", "state": 3}'
    ```
  
## What's next

Please take a look at [Getting Started](getting-started.md) to learn more about Scalar DL. 

## References

* [Getting started](getting-started.md)
* [Design document](design.md)
* [Javadoc for client SDK](https://scalar-labs.github.io/scalardl/javadoc/client/)
