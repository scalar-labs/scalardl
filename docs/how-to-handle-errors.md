> [!ATTENTION]
> 
> The `docs` folder has been moved to the centralized documentation repository, [docs-internal](https://github.com/scalar-labs/docs-internal). Please update this documentation in that repository instead.

# A Guide on How to Handle Errors in ScalarDL

This document sets out some guidelines for handling errors in ScalarDL.

## Basics

ScalarDL expects users to use [Client SDKs](https://github.com/scalar-labs/scalardl/blob/master/docs/index.md#client-sdks) to properly interact with ScalarDL system.
When an error occurs, the Client SDKs return an Exception (or an Error in Javascript-based SDKs) with a status code to users.
Users are expected to check the status code to identify the cause of errors.

## How to write error handling

Here, we explain the way to handle errors in more detail.

In Java Client SDK, the SDK throws a [ClientException](https://scalar-labs.github.io/scalardl/javadoc/latest/client/com/scalar/dl/client/exception/ClientException.html) so users can handle errors by catching the exception as follows:

```java
ClientService clientService = ...;
try {
    // interact with ScalarDL through a ClientService object
} catch (ClientException) {
    // e.getStatusCode() returns the status of the error
}
```

You can also handle errors similarly in Javascript-based Client SDKs.

```javascript
const clientService = ...; // ClientService object
try {

} catch (e) {
    // e.code returns the status of the error
    // e.message returns the error message
}

```

## Status codes

The status codes are grouped in five classes as similarly as HTTP status codes:

* Successful statues (200-299)
* Validation errors (300-399)
* User errors (400-499)
* Server errors (500-599)
* Client errors (600-699)

For more details, please check [StatusCode](https://scalar-labs.github.io/scalardl/javadoc/latest/common/com/scalar/dl/ledger/service/StatusCode.html).
