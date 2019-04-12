# How to start CA sever with CFSSL

This document describes how to start CA server with CFSSL.

## Prerequisites

We basically use CFSSL components for CA server and certificate handling.

- Golang (v1.8+) installation
- [cfssl & cfssljson](https://github.com/cloudflare/cfssl) installation

## Create a Root CA certificate

Create a CSR file in json format as follows.

```
[ca-csr.json]
{
  "CN": "Sample Root CA",
  "key": {
    "algo": "ecdsa",
    "size": 256
  },
  "names": [
    {
      "C":  "JP",
      "L":  "Tokyo",
      "O":  "Sample Root CA",
      "ST": "Tokyo"
    }
  ]
}
```

Generate a self-signed certificate and a private key based on the CSR.

```
$ cfssl gencert -initca ca-csr.json | cfssljson -bare ca

$ ls
ca-csr.json ca-key.pem  ca.csr      ca.pem
```

## Create a database for storing certificates

it needs a database for storing keys/certificate information.
CFSSL currently supports MySQL, PostgreSQL and SQLite.
We use SQLite this time for simplicity.

```
$ go get bitbucket.org/liamstask/goose/cmd/goose   
$ goose -path $GOPATH/src/github.com/cloudflare/cfssl/certdb/sqlite up
``` 

This will create a certstore_development.db in the current location.

## Create configration files for CA server

Configure signing algorithm, endpoint url and usages e imtermediate servers etc.
It assumes the server runs in a local environment only.

```
[cfssl-config.json]
{
  "signing": {
    "default": {
      "ocsp_url": "http://localhost:8889",
      "crl_url": "http://localhost:8888/crl",
      "expiry": "26280h",
      "usages": [
        "signing",
        "key encipherment",
        "client auth"
      ]
    },
    "profiles": {
      "ocsp": {
        "usages": ["digital signature", "ocsp signing"],
        "expiry": "26280h"
      },
      "intermediate": {
        "usages": ["cert sign", "crl sign"],
        "expiry": "26280h",
        "ca_constraint": {"is_ca": true}
      },
      "server": {
        "usages": ["signing", "key encipherment", "server auth"],
        "expiry": "26280h"
      },
      "client": {
        "usages": ["signing", "key encipherment", "client auth"],
        "expiry": "26280h"
      }
    }
  }
}
```
TODO: explore more about the configuration

Then, create a config file for database to point at the database file created in the previous section.
```
[db-config.json]
{
  "driver":"sqlite3",
  "data_source":"certstore_development.db"
}
```

### Create an intermediate CA certificate

This step is similar to generating root ca cert.

```
[server-ca.csr.json]
{
  "CN": "Sample Intermediate CA",
  "key": {
    "algo": "ecdsa",
    "size": 256
  },
  "names": [
    {
      "C":  "JP",
      "L":  "Tokyo",
      "O":  "Sample Intermediate CA",
      "ST": "Tokyo"
    }
  ]
}
```

The main differences are specifying the Root CA (`-ca -ca-key`) and the cfssl config (`-cfssl-config`) instead of `-initca` option.
```
$ cfssl gencert -ca ca.pem -ca-key ca-key.pem -config cfssl-config.json -profile "intermediate" server-ca.csr.json | cfssljson -bare ca-server
```

### Generate a OCSP server certificate

NOTE: OCSP is an internet protocol used for obtaining the revocation status of an X.509 digital certificate.

```
[ocsp.csr.json]
{
  "CN": "Sample OCSP",
  "key": {
    "algo": "ecdsa",
    "size": 256
  },
  "names": [
    {
      "C":  "JP",
      "L":  "Tokyo",
      "O":  "Sample OCSP",
      "ST": "Tokyo"
    }
  ]
}
```

```
$ cfssl gencert -ca ca-server.pem -ca-key ca-server-key.pem -config cfssl-config.json -profile "ocsp" ocsp.csr.json | cfssljson -bare server-ocsp
```

## Start servers

It's all ready now. Let's start the servers.

### CA server
```
$ cfssl serve -db-config db-config.json -ca-key ca-server-key.pem -ca ca-server.pem -config cfssl-config.json -responder server-ocsp.pem -responder-key server-ocsp-key.pem
```

### OCSP server
```
$ cfssl ocsprefresh -db-config db-config.json -responder server-ocsp.pem -responder-key server-ocsp-key.pem -ca ca-server.pem

// Bundle the Root CA and Intermediate CA
$ cat ca.pem ca-server.pem | tee bundle.pem

// Pre-generate the OCSP response
$ cfssl ocspdump -db-config db-config.json > ocspdump.txt

// Start the server
cfssl ocspserve -port=8889 -responses=ocspdump.txt
```

## References

- [cfssl & cfssljson](https://github.com/cloudflare/cfssl)
- [Setup Cloudflare CFSSL with OCSP responder](https://medium.com/@vrmvrm/setup-cloudflare-cfssl-with-ocsp-responder-aba44b4134e6)
- [Revoking certificates and running OCSP responder](https://propellered.com/2017/11/19/cfssl_revoking_certs_ocsp_reponder/)
