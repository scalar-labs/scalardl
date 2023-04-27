# ScalarDL Authentication Guide

This document explains the ScalarDL authentication mechanism and how to use it properly. 

## Authentication in ScalarDL

Authentication is one of the key roles in ScalarDL and makes the protocol work as expected.
ScalarDL uses authentication in the following three situations:
* Client authentication (for Ledger and Auditor)
    * Ledger and Auditor authenticate clients by using client-generated signatures attached to requests from the clients.
* Ledger authentication (for Auditor)
    * Auditor authenticates Ledger by using Ledger-generated signatures attached to [asset proofs](how-to-use-proof.md).
* Auditor authentication (for Ledger)
    * Ledger authenticates Auditor by using Auditor-generated signatures attached to client requests.

Note that Ledger authentication and Auditor authentication are only used in the Auditor mode. For more details about Auditor, see [Getting Started with ScalarDL Auditor](getting-started-auditor.md) and [ScalarDL Implementation](implementation.md).

Also, note that we use the term `signature` here to specify a byte array used for authentication.

## Authentication methods

ScalarDL supports two authentication methods: digital signatures and HMAC.
Both of these methods have advantages and disadvantages, as described below, but neither method sacrifices Byzantine fault detection capability.

### Digital signatures

* Advantages
  * Client requests, asset records, and asset proofs have the nonrepudiation property. Specifically, a digital signature attached to a client request is stored with the corresponding asset records that the request produces so that a client request and the corresponding records have the nonrepudiation property, i.e., we can ensure that the owner of the private key that signed the request created the records. Moreover, digital signatures attached to asset proofs that are returned to a client as the result of execution ensure that Ledger and Auditor created the proofs, respectively. If a client (application) keeps the proofs, the client can verify the results with the proofs as necessary.
* Disadvantages
  * Digital signatures are very slow. They will add nonnegligible performance overhead in exchange for the above benefits.

### HMAC

* Advantages
    * HMAC is much faster than digital signatures.
* Disadvantages
    * Client requests, asset records, and asset proofs do not have the nonrepudiation property.

### Which should I use?

If you do not require the nonrepudiation property, you should always use HMAC.
Technically, you could mix authentication methods, like using digital signatures for client authentication and HMAC for Ledger/Auditor authentication. However, because mixing methods can be very confusing, ScalarDL prohibits such usage.

Note that we plan to update ScalarDL to use only HMAC for Ledger and Auditor authentication for better performance. Similarly, we plan to unbundle Ledger and Auditor authentication from how we sign asset proofs. With the above changes, we will be able to return digitally signed asset proofs while using HMAC authentication between Ledger and Auditor.

## Configure

This section explains what variables you need to configure to use ScalarDL authentication properly. For details about each variable, see [Javadoc](TODO).

### Digital signatures

* Client authentication
    * Client-side properties
        * `scalar.dl.client.auditor.enabled` (set to `true` if you use Auditor)
        * `scalar.dl.client.authentication_method` (set to `digital-signature`)
        * `scalar.dl.client.entity.id` (or `scalar.dl.client.cert_holder_id`, which is deprecated.)
            * Used for identifying a client.
        * `scalar.dl.client.entity.identity.digital_signature.cert_pem` or `scalar.dl.client.entity.identity.digital_signature.cert_path` (or `scalar.dl.client.cert_pem` or `scalar.dl.client.cert_path`, which are deprecated.)
            * Used for registering a certificate for Ledger and Auditor to verify a client-generated signature.
            * See [this](https://github.com/scalar-labs/scalardl/blob/master/docs/ca/caclient-getting-started.md) for how to get a certificate.
        * `scalar.dl.client.entity.identity.digital_signature.cert_version` (or `scalar.dl.client.cert_version`, which is deprecated.)
        * `scalar.dl.client.entity.identity.digital_signature.private_key_pem` or `scalar.dl.client.entity.identity.digital_signature.private_key_path` (or `scalar.dl.client.private_key_pem` or `scalar.dl.client.private_key_path`, which are deprecated.)
            * Used for signing a request.
            * See [this](https://github.com/scalar-labs/scalardl/blob/master/docs/ca/caclient-getting-started.md) for how to get a private key.
        * `scalar.dl.client.entity.identity.digital_signature.cert_version` (or `scalar.dl.client.cert_version`, which is deprecated.)
    * Ledger-side properties
      * `scalar.dl.ledger.authentication.method` (set to `digital-signature`)
    * Auditor-side properties
      * `scalar.dl.auditor.authentication.method` (set to `digital-signature`)
* Ledger authentication 
    * Ledger-side properties
        * `scalar.dl.ledger.proof.enabled` (set to `true`)
            * Required because Ledger authentication uses the signatures of asset proofs.
        * `scalar.dl.ledger.proof.private_key_pem` or `scalar.dl.ledger.proof.private_key_path`  
            * Used for signing asset proofs.
            * See [this](https://github.com/scalar-labs/scalardl/blob/master/docs/ca/caclient-getting-started.md) for how to get a private key.
    * Auditor-side properties
        * `scalar.dl.auditor.ledger.cert_holder_id`
        * `scalar.dl.auditor.ledger.cert_version`
            * Used for verifying the signatures of asset proofs.
* Auditor authentication
    * Ledger-side properties
        * `scalar.dl.ledger.auditor.cert_holder_id`
          * Used for verifying an Auditor-generated signature attached to a client request.
        * `scalar.dl.ledger.auditor.cert_version`
          * Used for verifying an Auditor-generated signature attached to a client request.
    * Auditor-side properties
        * `scalar.dl.auditor.cert_holder_id`
          * Used for calling Ledger services directly by using the client library.
        * `scalar.dl.auditor.cert_version`
          * Used for calling Ledger services directly by using the client library.
        * `scalar.dl.auditor.private_key_pem` or `scalar.dl.auditor.private_key_path`
          * Used for signing a client request and a request from Auditor to Ledger.
          * See [this](https://github.com/scalar-labs/scalardl/blob/master/docs/ca/caclient-getting-started.md) for how to get a private key.

### HMAC

* Client authentication
    * Client-side properties
        * `scalar.dl.client.authentication_method` (set to `hmac`)
        * `scalar.dl.client.entity.id`
            * Used for identifying a client.
        * `scalar.dl.client.entity.identity.hmac.secret_key`
            * Used for signing a request.
            * A secret key should be a random, lengthy value (e.g., 32-character length hex string).
        * `scalar.dl.client.entity.identity.hmac.key_version`
    * Ledger-side properties
      * `scalar.dl.ledger.authentication.method` (set to `hmac`)
    * Auditor-side properties
      * `scalar.dl.auditor.authentication.method` (set to `hmac`)

* Ledger and Auditor authentication
    * Ledger-side properties
        * `scalar.dl.ledger.proof.enabled` (set to `true`)
            * Required because Ledger authentication uses the signatures of asset proofs.
        * `scalar.dl.ledger.servers.authentication.hmac.secret_key`
            * Used for signing and verifying messages and requests between Ledger and Auditor.
            * A secret key should be a random, lengthy value (e.g., 32-character length hex string).
        * `scalar.dl.ledger.authentication.hmac.cipher_key`
            * Used for encrypting and decrypting the secret keys of clients.
            * A cipher key should be an unpredictable, lengthy value.
    * Auditor-side properties
        * `scalar.dl.auditor.servers.authentication.hmac.secret_key`
            * Used for signing and verifying messages and requests between Ledger and Auditor. Must be the same key as `scalar.dl.ledger.servers.authentication.hmac.secret_key`.
            * A secret key should be a random, lengthy value (e.g., 32-character length hex string).
        * `scalar.dl.auditor.authentication.hmac.cipher_key`
            * Used for encrypting and decrypting the secret keys of clients.
            * A cipher key should be an unpredictable, lengthy value.

## Prepare before executing requests

 After configuration, you must do the following to prepare for issuing execution requests.

### Digital signatures

* Register clients' certificates to Ledger (and Auditor, if enabled).
    * Use the client library or the command-line tool (`register-cert`).
* Register Auditor's certificates to Ledger.
    * Required only if Auditor is enabled.
* Register Ledger's certificates to Auditor.
    * Required only if Auditor is enabled.
* Register contracts to Ledger (and Auditor, if enabled).

### HMAC

* Register clients' secret keys to Ledger (and Auditor, if enabled).
  * Use the client library or the command-line tool (`register-secret`).
* Register contracts to Ledger (and Auditor).