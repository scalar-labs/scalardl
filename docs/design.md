# Scalar DL v1 design document

## Introduction

Scalar DL is a blockchain-inspired distributed ledger. This design document briefly explains the background, design and implementation of Scalar DL.

## Background and Objectives

Distributed ledgers or blockchains have been attracting a lot of attention recently, especially in the areas of financial and legal applications.
They have gained acceptance due to their tamper-evidence and decentralized control properties.
However, the existing platforms do not necessarily handle properties such as finality and scalability well, which are particularly important for mission-critical applications.
HyperLedger fabric [1] applies blockchain to a private network owned by the fixed number of organizations so that ledger states are always finalized unless malicious attacks happen. 
But its architecture inherently focuses on realtime tamper-evidence over scalability due to its endorsement mechanism so that its performance does not necessarily scale as the number of peers increases.
Also, because it is designed toward a general distributed ledger, its complexity is becoming quite high and developers and administrators may have a lot of difficulties using it properly.
Scalar DL is a simple and practical solution to solve such issues in an essentially different approach.

## Design Goals

The primary design goals of Scalar DL are to achieve both high tamper-evidence of data and high scalability of performance. We have also taken great care to provide ACID-compliance, exact finality, linearizable consistency, and high availability.
The performance of Scalar DL is highly dependent on the underlying database performance, but it can be modified without much effort by replacing the underlying database with one that is suitable for the user's needs because of its loosely-coupled architecture.
Ease of use and simplicity are also part of our main design goals since they are the keys to making Scalar DL scalable.

## Fault Model

The assumed fault model behind Scalar DL is byzantine fault [2].
However, with some configurations, it only assumes weak (limited) byzantine fault; that is, the database component assumes byzantine fault but the ledger component assumes only crash fault.

## Data Model

Scalar DL abstracts data as a set of assets. An asset can be arbitrary data but is more compatible to being viewed as a historical series of data.
For example, assets can range from the tangible (real estate and hardware) to the intangible (contracts and intellectual property).

An asset is composed of one or more asset records where each asset record is identified by an asset ID and an age.
An asset record with age M has a cryptographic hash of the previous asset record with age M-1, forming a hash-chain, so that removing or updating an intermediate asset record may be detected by traversing the chain.

There is also a chain structure in between multiple assets. 
This chain is a relationship constructed by business/application logic.
For example in a banking application, payment in between multiple accounts would update the both accounts, which will create such a relationship between assets. 
In Scalar DL, business logic is digitally signed and tamper evident, and the initial state of an asset is the empty state, which is also regarded as tamper-evident, so that we can deduce the intermediate asset state is also tamper evident as shown below.

```
Sn = F (Sn-1) 

Si: the state of a set of asset at age i
F: the signed business logic
```

Thus, assets in Scalar DL can be seen as a DAG of dependencies.

## Smart Contract

Scalar DL defines a digitally signed business logic as a `Smart Contract`, which only a user with access to the signer's private key can execute.
This makes the system easier to detect tampering because the signature can be made only by the owners of private keys.

## Implementation

### High-level Architecture

WIP: software stack

Scalar DL is composed of 3 layers. 
The bottom layer is called `Ledger`. It mainly executes contracts and manages assets. It uses Scalar DB as a data and transaction manager, but also abstracts such management so that the implementation can be replaced with other database implementations.
The middle layer is called `Ordering`. It orders contract execution requests in a deterministic way, so that multiple independent organizations will receive requests in the same order. It is similar to HyperLedger fabric's `Orderer`, but it differs in the way it does the processing.
The top layer is called `Client SDK`. It is a client-facing library composed of a set of Java programs to interact with either `Ordering`, or `Ledger`.

The basic steps of contract execution is as follows:
1. client programs interacting with the Client SDK request one or more execution of contracts to Ordering
2. Ordering orders the requests and pass them to Ledger
3. Ledger executes the requests in the order given from the Ordering

### Smart Contract as a Distributed Transaction

Scalar DL executes a contract as a distributed transaction of the underlining database system (Scalar DB at the moment).
More specifically, a contract (or a set of contracts invoked in one execution request) is composed of multiple reads and writes from/to assets, and those reads and writes are treated as single distributed transaction, so that they are atomically executed, consistently and durably written, and isolated from other contract executions.

A Smart Contract in Scalar DL is a java program which extends the base class `Contract`.

### Determinism management by Ordering

Scalar DL pre-orders contract execution requests before execution so that multiple independent organizations receive the requests in the same order and can make their states the same as others' without having to interact with each other.
It uses Kafka as the ordering manager because of its reliability and performance.

## Key Features

### Client-side Proof

This is for tamper-evidence enhancement.
(omitted because it is patent-pending)

### Decoupled Consensus

This is for scalable contract execution.
(omitted because it is patent-pending)

### Partial-order-aware Execution

This is for scalable contract execution.
(omitted because it is patent-pending)

## Future Work

WIP

## References

- [1] Elli Androulaki, Artem Barger, Vita Bortnikov, Christian Cachin, Konstantinos Christidis, Angelo De Caro, David Enyeart, Christopher Ferris, Gennady Laventman, Yacov Manevich, Srinivasan Muralidharan, Chet Murthy, Binh Nguyen, Manish Sethi, Gari Singh, Keith Smith, Alessandro Sorniotti, Chrysoula Stathakopoulou, Marko Vukolić, Sharon Weed Cocco, Jason Yellick, Hyperledger fabric: a distributed operating system for permissioned blockchains, Proceedings of the Thirteenth EuroSys Conference, April 23-26, 2018, Porto, Portugal.
- [2] Leslie Lamport, Robert Shostak, Marshall Pease, The Byzantine Generals Problem, ACM Transactions on Programming Languages and Systems (TOPLAS), v.4 n.3, p.382-401, July 1982.
