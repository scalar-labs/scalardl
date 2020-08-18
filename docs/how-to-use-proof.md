# A Guide on How to Use Asset Proofs in Scalar DL

This document sets out some guidelines for using Asset Proofs in Scalar DL.

## What is an Asset Proof in Scalar DL?

An Asset Proof in Scalar DL is a set of information about an asset record that is created along with the asset. It is composed of the following records so that it can be treated as evidence of the creation of an asset record by a ledger.

- The ID of an asset record
- Age of the asset record
- Nonce of the execution request that creates the asset record
- A cryptographic hash of the asset record
- Digita sigunature of the above four records 

## The Benefits of Asset Proofs

Since Asset Proofs are evidence at the time of execution by a ledger, it is hard for the leger to make a lie (tamper data) after the evidence is created because the proofs and the ledger states would be inconsistent.
Thus, using and managing Asset Proofs appropriately could reduce the risk of tampering of data, so it could make Scalar DL more tamper-evident without creating additional computational resources such as ordering components.
However, the ledger can make a lie from the beginning and it makes the proofs insufficient to detect the tampering.

## How to use Asset Proofs

You can get Asset Proofs from the result of the `executeContract` method of the SDKs. Please check [the documents](https://github.com/scalar-labs/scalardl#client-sdks) of client SDKs for more detail.
It is recommended to store the proofs outside of a domain in which the ledger is managed so that malicious activities in one domain can be detected by the other domain.
It is worth considering storing proofs in cloud storages for ease of management.
