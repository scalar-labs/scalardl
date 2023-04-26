# A Guide on ScalarDL Authentication

This document explains the ScalarDL authentication mechanism and how to use it properly. 

## Authentication in ScalarDL

Authentication is one of the key roles in ScalarDL to make the protocol work as expected.
ScalarDL uses authentication for the following three places:
* Client authentication (for Ledger and Auditor)
    * Ledger and Auditor authenticates clients using client-generated signatures attached to requests from the clients.
* Ledger authentication (for Auditor)
    * Auditor authenticates Ledger using Ledger-generated signatures attached to [Asset Proofs](how-to-use-proof.md).
* Auditor authentication (for Ledger)
    * Ledger authenticates Auditor using Auditor-generated signatures attached to client requests.

Note that Ledger authentication and Auditor authentication are only used in the Auditor mode. For more details about Auditor, please see the [Getting Started with ScalarDL Auditor](getting-started-auditor.md) and [ScalarDL Implementation](implementation.md).

Also, note that we use the term `signature` here to specify a byte array used for authentication.

## Authentication methods

ScalarDL supports two authentication methods: Digital Signatures and HMAC.
The methods have advantages and disadvantages, as shown below, but Byzantine fault detection capability won't be sacrificed either way.

### Digital Signatures
* Advantages
  * Asset records and asset proofs have non-repudiation property. Specifically, a digital signature attached to a request is stored with the corresponding asset records produced by the request so that the records have non-repudiation property, i.e., we can ensure that the records are created by the owner of the private key that signed the request. Moreover, digital signatures attached to asset proofs returned to a client as the result of execution ensure that the proofs are created by Ledger and Auditor, respectively. If a client (application) keeps the proofs, the client can verify results with the proofs as necessary.
* Disadvantages
  * Digital Signatures are very slow. They will add non-negligible performance overhead in exchange for the above benefits.

### HMAC
* Advantages
    * HMAC is way faster than digital signatures.
* Disadvantages
    * Asset records and asset proofs do not have non-repudiation property.

### Which should I use?

If you do not require non-repudiation property, you should always use HMAC.
Technically, you could mix authentication methods, e.g., use digital signatures for client authentication and use HMAC for Ledger/Auditor authentication, but it is very confusing; thus, ScalarDL prohibits such usage.

Note that we plan to update ScalarDL to use only HMAC for Ledger and Auditor authentication for better performance. Similarly, we plan to unbundle Ledger (Auditor) authentication from how we sign asset proofs. With the above changes, we will be able to return digitally-signed asset proofs while using HMAC authentication between Ledger and Auditor.

## Configure

This section explains what variables you need to configure to use the authentication of ScalarDL properly. For the detail of each variable, please also check the [Javadoc](TODO).

### Digital Signatures

* Client authentication
    * Client-side properties
        * `scalar.dl.client.auditor.enabled` (set true if you use Auditor)
        * `scalar.dl.client.authentication_method` (set "digital-signature")
        * `scalar.dl.client.entity.id` (or `scalar.dl.client.cert_holder_id`, which is deprecated.)
            * It is used for identifying a client.
        * `scalar.dl.client.entity.identity.digital_signature.cert_path` or `scalar.dl.client.entity.identity.digital_signature.cert_pem` (or `scalar.dl.client.cert_pem` or `scalar.dl.client.cert_path`, which are deprecated.)
            * It is used for registering a certificate for Ledger and Auditor to verify a client-generated signature.
        * `scalar.dl.client.entity.identity.digital_signature.cert_version` (or `scalar.dl.client.entity.identity.digital_signature.cert_version`, which is deprecated.)
        * `scalar.dl.client.entity.identity.digital_signature.private_key_pem` or `scalar.dl.client.entity.identity.digital_signature.private_key_path` (or `scalar.dl.client.private_key_pem` or `scalar.dl.client.private_key_path`, which are deprecated.)
            * It is used for signing a request.
    * Ledger-side properties
      * `scalar.dl.ledger.authentication.method` (set "digital-signature")
    * Auditor-side properties
      * `scalar.dl.auditor.authentication.method` (set "digital-signature")
* Ledger authentication 
    * Ledger-side properties
        * `scalar.dl.ledger.proof.enabled` (set true)
            * It is required because Ledger authentication uses the signatures of Asset Proofs.
        * `scalar.dl.ledger.proof.private_key_pem` or `scalar.dl.ledger.proof.private_key_path`  
            * It is used for signing Asset Proofs.
    * Auditor-side properties
        * `scalar.dl.auditor.ledger.cert_holder_id`
        * `scalar.dl.auditor.ledger.cert_version`
            * It is used for verifying the signatures of Asset Proofs.
* Auditor authentication
    * Ledger-side properties
        * `scalar.dl.ledger.auditor.cert_holder_id`
          * It is used for verifying an Auditor-generated signature attached to a client request.
        * `scalar.dl.ledger.auditor.cert_version`
          * It is used for verifying an Auditor-generated signature attached to a client request.
    * Auditor-side properties
        * `scalar.dl.auditor.cert_holder_id`
          * It is used for calling Ledger services directly using the client library.
        * `scalar.dl.auditor.cert_version`
          * It is used for calling Ledger services directly using the client library.
        * `scalar.dl.auditor.private_key_pem` or `scalar.dl.auditor.private_key_path`
          * It is used for signing a client request and a request from Auditor to Ledger.

### HMAC

* Client authentication
    * Client-side properties
        * `scalar.dl.client.authentication_method` (set "hmac")
        * `scalar.dl.client.entity.id`
            * It is used for identifying a client.
        * `scalar.dl.client.entity.identity.hmac.secret_key`
            * It is used for signing a request.
        * `scalar.dl.client.entity.identity.hmac.key_version`
    * Ledger-side properties
      * `scalar.dl.ledger.authentication.method` (set "hmac")
    * Auditor-side properties
      * `scalar.dl.auditor.authentication.method` (set "hmac")

* Ledger and Auditor authentication
    * Ledger-side properties
        * `scalar.dl.ledger.proof.enabled` (set true)
            * It is required because Ledger authentication uses the signatures of Asset Proofs.
        * `scalar.dl.ledger.authentication.hmac.cipher_key`
            * It is used for signing and verifying messages/requests between Ledger and Auditor.
    * Auditor-side properties
        * `scalar.dl.auditor.authentication.hmac.cipher_key`
            * It is used for signing and verifying messages/requests between Ledger and Auditor. It must be the same key as `scalar.dl.ledger.authentication.hmac.cipher_key`.

 ## Prepare before executing requests

 After configuration, you must do the following to prepare for issuing execution requests.

### Digital Signatures
* Register clients' certificates to Ledger (and Auditor).
    * Use the client library or the command-line tool (`register-cert`).
* Register contracts to Ledger (and Auditor).
* Register Auditor's certificates to Ledger.
    * It is needed only when Auditor is enabled.
* Register Ledger's certificates to Auditor.
    * It is needed only when Auditor is enabled.

### HMAC
* Register clients' secret keys to Ledger (and Auditor).
  * Use the client library or the command-line tool (`register-secret`).
* Register contracts to Ledger (and Auditor).