# ScalarDL Escrow payment CLI

The following is a simple Java CLI application to try out and test [ScalarDL](https://github.com/scalar-labs/scalardl). PicoCLI is used as a CLI framework.

ScalarDL has support and SDK for multiple languages. In this example, JAVA is used.

As this is just a sample project, all commands can be run via Gradle. This is not a production-ready example and is only provided as study material.

Please check out the mentioned Makefile commands and the official ScalarDL documentation to see which commands are being executed.



## Introduction

### Escrow

Instead of directly transferring the funds from account A to account B an intermediary account is
introduced. The funds are locked in this intermediary account until both parties agreed that the
transaction is completed. Should something go wrong both the buyer and the seller cannot access the
funds until the issue is resolved.

The following is a typical Escrow payment flow:

- Registered buyer with account A, buys an item from a registered seller with account B
- An order entry is created and the order is set to the `open` status
- An escrow account between the two parties is created
- Account A is credited and the funds are moved to the escrow account
- The seller updates the order and notifies of shipment of the package
- The order is updated and set to the `shipped` status
- After receiving the package, the buyer updates the order and marks it as received
- The Escrow account is credited and the funds are moved to account B.
- The order is closed and marked with status `complete`

Order cancellation is possible but only while the order is still in the `open` status. After the
goods are shipped, the order cannot be cancelled in the current implementation



## Features

This CLI application provides to most important actions to simulate an escrow payment flow. Extra
actions are supported for data lookup and monitoring.

The following actions are supported:

**Account**

- register a new account
- add funds, charge, an account
- view account history

**Item**

- register a new item
- view item details

**Order**

- create a new order
- Mark an order as shipped
- Mark and order as received (complete an order)
- Cancel an order
- View order history

**Escrow Account**

- View escrow account history



## Prerequisites

- Docker

- Makefile

- Java



## Setup

### ScalarDL

The code to start up the ScalarDL server is not included in this repository.  To get ScalarDL up and running quickly, please clone the [following repository](https://github.com/scalar-labs/scalardl-samples) and follow the provided instructions on how to get started with ScalarDL.

### Certificate

An example certificate is used but is enough for the example. The certiticate can be loaded
via `make register-certificate`

### Contracts

All the contract names have an added suffix. This is the name of the certificate owner which in this
case is `foo`. This is not required and only done to make sure the contract names are unique.

The contracts can be loaded via `make register-contracts`

### Client SDK

The Java client SDK requires a `client.properties` file to work. Currently the same settings as the
scalar examples projects are used.

### Test scenarios

Two example test scenarios are provided to see the payment and order cancellation flow in practice.

The scenarios can be run via `make run-complete-order-scenario` and `make run-cancel-order-scenario`



## Usage

### Build

The application can be build via `./gradlew build` or by running the `make build` Makefile command.

### CLI commands

#### Account

Management of the accounts via CLI.

##### Create account

Create a new account by supplying the `id` which is string and can be anything. The account
automatically will have it's `balance` set to 0 and `created_at` set to the current timestamp.

*Command*

```
./gradlew run --args="account add -id <account id> -n <name>"
```

##### View account history

Retrieve the account history.

*Command*

```
./gradlew run --args="account view -id <account id>"
```

##### Charge account

Add funds to the account's balance.

*Command*

```
./gradlew run --args="account charge -id <account id> -a <amount>"
```

#### Item

Management of the items via CLI.

##### Create item

Create a new item by providing an `id`, a `price` and `seller`, which is the account Id, and an
item `name`.

*Command*

```
./gradlew run --args="item add -id <item id> -p <price> -s <seller account id -n <item name>"
```

##### View item

View item details

*Command*

```
./gradlew run --args="item view -id <item id>"
```

#### Order

Management of the orders via CLI.

##### Create order

Create a new order by providing an `id`, a `item id` and `buyer`, which is the account Id.

*Command*

```
./gradlew run --args="order add -id <order id> -b <buyer account id> -i <item id>"
```

##### Cancel order

Cancel an order that is currently in `open` status. Orders cannot be cancelled anymore when in
another state. A `account id` is required and validated. If the order does not belong to the
specified account, it can not be cancelled.

*Command*

```
./gradlew run --args="order cancel -id <order id> -a <account id>"
```

##### Mark order item as shipped

Update the order as a seller to notify the buyer the item has been shipped.

*Command*

```
./gradlew run --args="order shipped -id <order id> -s <seller account id>"
```

##### Mark order item as received

Update the order as a buyer to notify the seller the item has been received. The order will be
completed.

*Command*

```
./gradlew run --args="order received -id <order id> -b <buyer account id>"
```

##### View order history

View the history for one order.

*Command*

```
./gradlew run --args="order view -id <order id>"
```

#### Escrow account

Management of the escrow accounts via CLI.

##### View escrow account history

View the activity and update history for one specific escrow account between buyer and seller

*Command*

```
./gradlew run --args="escrow view -b <buyer account id> -s <seller account id>"
```

