SHELL := /bin/bash

##### BUILD
.PHONY: build
build:
	@echo "Building scalar DL escrow demo CLI"
	@./gradlew build

##### ENVIRONMENT
.PHONY: env-up env-up env-reset
env-up:
	@echo "Starting environment"
	@docker-compose -f docker-compose.yml up --force-recreate --build -d
	@echo "Environment up"

env-down:
	@echo "Stop environment ..."
	@docker-compose -f docker-compose.yml down -v
	@echo "Environment down"

env-reset: env-down env-up


#### Scalar DL
.PHONY: register-cert assemble register-contracts clear register-all

register-all: register-cert register-contracts

register-cert:
	@echo "Registering certificate ..."
	@client/bin/register-cert --properties client.properties

assemble: clear
	@echo "Building contracts ..."
	@./gradlew assemble

register-contracts: assemble
	@echo "Registering contracts ..."
	@client/bin/register-contract --properties client.properties --contract-id AddAccount_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.AddAccount --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/AddAccount.class
	@client/bin/register-contract --properties client.properties --contract-id AddItem_foo --contract-binary-name com.ypeckstadt.escrow.contract.item.AddItem --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/item/AddItem.class
	@client/bin/register-contract --properties client.properties --contract-id AddOrder_foo --contract-binary-name com.ypeckstadt.escrow.contract.order.AddOrder --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/order/AddOrder.class
	@client/bin/register-contract --properties client.properties --contract-id CancelOrder_foo --contract-binary-name com.ypeckstadt.escrow.contract.order.CancelOrder --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/order/CancelOrder.class
	@client/bin/register-contract --properties client.properties --contract-id ChargeAccount_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.ChargeAccount --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/ChargeAccount.class
	@client/bin/register-contract --properties client.properties --contract-id SetOrderToReceivedStatus_foo --contract-binary-name com.ypeckstadt.escrow.contract.order.SetOrderToReceivedStatus --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/order/SetOrderToReceivedStatus.class
	@client/bin/register-contract --properties client.properties --contract-id SetOrderToShippedStatus_foo --contract-binary-name com.ypeckstadt.escrow.contract.order.SetOrderToShippedStatus --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/order/SetOrderToShippedStatus.class
	@client/bin/register-contract --properties client.properties --contract-id ViewAccountHistory_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.ViewAccountHistory --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/ViewAccountHistory.class
	@client/bin/register-contract --properties client.properties --contract-id ViewItem_foo --contract-binary-name com.ypeckstadt.escrow.contract.item.ViewItem --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/item/ViewItem.class
	@client/bin/register-contract --properties client.properties --contract-id DebitEscrowAccount_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.DebitEscrowAccount --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/DebitEscrowAccount.class
	@client/bin/register-contract --properties client.properties --contract-id CreditEscrowAccount_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.CreditEscrowAccount --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/CreditEscrowAccount.class
	@client/bin/register-contract --properties client.properties --contract-id CreditAccount_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.CreditAccount --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/CreditAccount.class
	@client/bin/register-contract --properties client.properties --contract-id DebitAccount_foo --contract-binary-name com.ypeckstadt.escrow.contract.account.DebitAccount --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/account/DebitAccount.class
	@client/bin/register-contract --properties client.properties --contract-id ViewOrderHistory_foo --contract-binary-name com.ypeckstadt.escrow.contract.order.ViewOrderHistory --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/order/ViewOrderHistory.class
	@client/bin/register-contract --properties client.properties --contract-id ViewEscrowAccountHistory_foo --contract-binary-name com.ypeckstadt.escrow.contract.escrowAccount.ViewEscrowAccountHistory --contract-class-file build/classes/java/main/com/ypeckstadt/escrow/contract/escrowAccount/ViewEscrowAccountHistory.class

clear:
	@rm -rf build


#### Scenario test
.PHONY: run-complete-order-scenario run-cancel-order-scenario all
run-complete-order-scenario:
	@./gradlew run --args="account add -id buyer_1 -n Alfred"
	@./gradlew run --args="account add -id seller_1 -n Yves"
	@./gradlew run --args="account charge -id buyer_1 -a 5000"
	@./gradlew run --args="item add -id item_1 -n bicycle -p 6000 -s seller_1"
	@./gradlew run --args="order add -id order_1 -b buyer_1 -i item_1"
	@./gradlew run --args="account charge -id buyer_1 -a 5000"
	@./gradlew run --args="order add -id order_1 -b buyer_1 -i item_1"
	@./gradlew run --args="order shipped -id order_1 -s seller_1"
	@./gradlew run --args="order received -id order_1 -b buyer_1"
	@./gradlew run --args="order view -id order_1"
	@./gradlew run --args="account view -id buyer_1"
	@./gradlew run --args="account view -id seller_1"
	@./gradlew run --args="escrow view -b buyer_1 -s seller_1"

run-cancel-order-scenario:
	@./gradlew run --args="account add -id buyer_1 -n Alfred"
	@./gradlew run --args="account add -id seller_1 -n Yves"
	@./gradlew run --args="account charge -id buyer_1 -a 50000"
	@./gradlew run --args="item add -id item_1 -n bicycle -p 10000 -s seller_1"
	@./gradlew run --args="order add -id order_1 -b buyer_1 -i item_1"
	@./gradlew run --args="account view -id buyer_1"
	@./gradlew run --args="order cancel -id order_1 -a buyer_1"
	@./gradlew run --args="order view -id order_1"
	@./gradlew run --args="account view -id buyer_1"





