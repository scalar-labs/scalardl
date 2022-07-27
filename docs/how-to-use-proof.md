# A Guide on How to Use Asset Proofs in Scalar DL

This document sets out some guidelines for using Asset Proofs in Scalar DL.

## What is an Asset Proof in Scalar DL?

An Asset Proof in Scalar DL is a set of information about an asset record and used as evidence of the existence of the asset record. It is composed of the following items.

- ID of an asset record
- Age of the asset record
- Nonce of the execution request that creates the asset record
- A cryptographic hash of the asset record
- A cryptographic hash of the previous age's asset record, if any
- The digital signature of the above entries

## The Benefits of Asset Proofs

Since Asset Proofs are evidence at the time of execution by the Ledger, it is hard for the Ledger to tamper data after the evidence is created because the proofs and the Ledger states would be diverged.
Thus, making use of Asset Proofs appropriately could reduce the risk of tampering of data.

## How to use Asset Proofs

You can get Asset Proofs ([AssetProof](https://scalar-labs.github.io/scalardl/javadoc/latest/common/com/scalar/dl/ledger/asset/AssetProof.html)) from the result ([ContractExecutionResult](https://scalar-labs.github.io/scalardl/javadoc/latest/common/com/scalar/dl/ledger/model/ContractExecutionResult.html)) of the `executeContract` method of the SDKs. A proof can be validated if it is not tampered and it is from the Ledger by verifying the signature.

It is recommended to store the proofs outside of a domain in which the Ledger runs so that malicious activities in one domain can be detected by the other domain. It is worth considering storing proofs in cloud storages for ease of management.

The proofs obtained in execution can be utilized when you do `validateLedger`. `validateLedger` also returns the proof of a specified asset record after doing Ledger-side validation. Then, the client can check if the proof is the same as the one that have returned from the Ledger before.

## Scalar DL Auditor

Scalar DL Auditor utilizes Asset Proofs to achieve the Byzantine fault detection feature. For more details about Scalar DL Auditor, please read [Getting Started with Scalar DL Auditor](getting-started-auditor.md).
