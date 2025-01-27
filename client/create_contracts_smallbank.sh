#!/bin/sh

build/install/client/bin/register-contract --properties client.properties --contract-id create_account --contract-binary-name com.example.contract.smallbank.CreateAccount --contract-class-file /Users/hiroyuki/git/caliper-benchmarks/src/scalardl/build/classes/java/main/com/example/contract/smallbank/CreateAccount.class
build/install/client/bin/register-contract --properties client.properties --contract-id transact_savings --contract-binary-name com.example.contract.smallbank.TransactSavings --contract-class-file /Users/hiroyuki/git/caliper-benchmarks/src/scalardl/build/classes/java/main/com/example/contract/smallbank/TransactSavings.class
build/install/client/bin/register-contract --properties client.properties --contract-id deposit_checking --contract-binary-name com.example.contract.smallbank.DepositChecking --contract-class-file /Users/hiroyuki/git/caliper-benchmarks/src/scalardl/build/classes/java/main/com/example/contract/smallbank/DepositChecking.class
build/install/client/bin/register-contract --properties client.properties --contract-id send_payment --contract-binary-name com.example.contract.smallbank.SendPayment --contract-class-file /Users/hiroyuki/git/caliper-benchmarks/src/scalardl/build/classes/java/main/com/example/contract/smallbank/SendPayment.class
build/install/client/bin/register-contract --properties client.properties --contract-id write_check --contract-binary-name com.example.contract.smallbank.WriteCheck --contract-class-file /Users/hiroyuki/git/caliper-benchmarks/src/scalardl/build/classes/java/main/com/example/contract/smallbank/WriteCheck.class
build/install/client/bin/register-contract --properties client.properties --contract-id amalgamate --contract-binary-name com.example.contract.smallbank.Amalgamate --contract-class-file /Users/hiroyuki/git/caliper-benchmarks/src/scalardl/build/classes/java/main/com/example/contract/smallbank/Amalgamate.class
