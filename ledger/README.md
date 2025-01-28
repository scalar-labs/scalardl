## ScalarDL

### Build & Install

To build and install the ScalarDL daemon, use gradle installDist, which will build the source files and install an executable, required jars, and security policy file (src/dist/security.policy) into build/install/ledger directory.

```
$ ./gradlew installDist
```

### Run

This runs ScalarDL with the security policy specified at build/install/ledger/security.policy

```
$ cd build/install/ledger
$ bin/scalar-ledger
```
