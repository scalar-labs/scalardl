# How to get a certificate

This document describes how to get a certificate to enroll in DLT network.

## Prerequisites

We basically use CFSSL components for CA server and certificate handling.

- Golang (v1.8+) installation
- openssl installation
- [cfssl & cfssljson](https://github.com/cloudflare/cfssl) installation

### Generate a private key and a CSR

```
$ openssl ecparam -name prime256v1 -out prime256v1.pem
$ openssl req -new -newkey ec:prime256v1.pem -nodes -keyout client-key.pem.pkcs8 -out client.csr
$ openssl ec -in client-key.pem.pkcs8 -out client-key.pem
```

or

```
$ cat << EOF > client-cert.json
{
    "CN": "client.example",
    "key": {
        "algo": "ecdsa",
        "size": 256
    },
    "names": [
        {
            "O": "Client Example",
            "L": "Shinjuku",
            "ST": "Tokyo",
            "C": "JP"
        }
    ]
}
EOF

$ cfssl selfsign "" ./client-cert.json | cfssljson -bare client
$ ls -1
client-cert.json
client-key.pem
client.csr
client.pem
```

### Get a certificate from a CA Server

```
cfssl sign -remote "localhost:8888" -profile "client" client.csr | cfssljson -bare client -
```
- NOTE: `-remote` option is needed to specify the CA endpoint URI
- NOTE: `-bare` option for cfssljson is needed to specify a prefix of output key files

You will get a certificate named `client.pem` from CA, and are almost ready to execute your contracts.

