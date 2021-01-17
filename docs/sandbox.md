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

Please note that we generate a pair of a private key and a certificate and register the certificate for ease of use for the Sandbox environment, but those steps are usually required in your own environment.

## Before running your first contract 

Please make sure you have the [Oracle JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed, and, if you haven't already, download the Scalar DL Java Client SDK.

```
$ git clone https://github.com/scalar-labs/scalardl-java-client-sdk.git
$ cd scalardl-java-client-sdk
$ git checkout v2.0.8
```

Scalar DL manages data as a set of assets. Each asset is identified by its `asset_id`, an arbitrary, but unique, string specified by a user to manage the asset, and consists of its change history. Each history item is made up of the `asset_id`, an `age` (starting from zero), and `data` at the specified age.

Since the Sandbox is a shared environment that anyone can access,
you need to take special care of choosing names for your `asset_id`s not to conflict with the `asset_id`s chosen by other users.
One recommended way to do this is to append your username to the asset name, for example, `<username>-<your-asset-name>`.
Similarly, the same care is needed when choosing contract binary names and contract IDs and we recommend using `<username>.<your-contract-class-name>` and `<username>-<your-contract-class-name>` respectively.
Lastly, please use Java client SDK v2.0.8 at the moment.

## Run the StateUpdater contract

We will run the contract [`StateUpdater.java`](https://github.com/scalar-labs/scalardl-java-client-sdk/blob/master/src/main/java/com/org1/contract/StateUpdater.java), which manages status of some asset.

1. Update the contract

    Update the package of [`StateUpdater.java`](https://github.com/scalar-labs/scalardl-java-client-sdk/blob/master/src/main/java/com/org1/contract/StateUpdater.java) from `com.org1.contract` to `<username>`.
    Use your favorite IDE to change the package or you can change it manually by updating the source file and move the file to an appropriate directory.
    For example, if your username is `foo`, then the first line of the source file should be `package foo;` and the source file is located at `scalardl-java-client-sdk/src/main/java/foo/StateUpdate.java`.

    ```bash
    $ USERNAME=<username>
    # Replace "package com.org1.contract" to "package <username>"
    $ $EDITOR src/main/java/com/org1/contract/StateUpdater.java
    $ mkdir -p "src/main/java/${USERNAME}"
    $ mv src/main/java/com/org1/contract/StateUpdater.java "src/main/java/${USERNAME}/"
    ```

2. Compile the contract

    ```bash
    $ ./gradlew assemble
    ```

    This will generate `build/classes/java/main/<username>/StateUpdater.class`.

3. Register the contract

    ```bash
    $ client/bin/register-contract --properties client.properties --contract-id "${USERNAME}-StateUpdater" --contract-binary-name "${USERNAME}.StateUpdater" --contract-class-file "build/classes/java/main/${USERNAME}/StateUpdater.class"
    ```

    The `client.properties` should be the same file from the zip downloaded earlier.

4. Execute the contract

    ```bash
    $ client/bin/execute-contract --properties client.properties --contract-id "${USERNAME}-StateUpdater" --contract-argument "{'asset_id': '${USERNAME}-myasset', 'state': 3}"
    ```
 
## What's next

Please take a look at [Getting Started](getting-started.md) to learn more about Scalar DL. 

## References

* [Getting started](getting-started.md)
* [Design document](design.md)
* [Javadoc for client SDK](https://scalar-labs.github.io/scalardl/javadoc/client/)
