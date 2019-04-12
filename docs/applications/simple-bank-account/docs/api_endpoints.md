# API endpoints

## `GET v1/accounts`

- **Not implemented yet**
- Return a list of accounts and their balances as a JSON array

```
[
    {
        "account": <id>,
        "balance": <balance>
    },
    ...
]
```

## `GET v1/accounts/{id}?start=<num>&end=<num>&order=<asc or desc>&limit=<num>`

- Return the given account history as a JSON array
- Return `200 OK` if success

```
[
    {
        "account": <id>,
        "balance": <balance>,
        "age": <age>
    },
    ...
]
```

## `PUT v1/accounts/{id}`

- Create the specified account with id={id}
- Return `200 OK` if success
- Return `403 Bad Request` if the account already exists

## `POST v1/accounts/{id}/deposit?amount=<amount>`

- Deposit into a specified account
- Return `200 OK` if success

## `POST v1/accounts/{id}/withdraw?amount=<amount>`

- Withdraw from a specified account
- Return `200 OK` if success
- Return `403 Bad Request` if amount exceeds the balance in the account

## `POST v1/transfers?from=<id>&to=<id>&amount=<amount>`

- Transfer funds from one account to another
- Return `200 OK` if success
- Return `403 Bad Request` if amount exceeds the balance in the from account

## Delete an account

There is no way to do this.