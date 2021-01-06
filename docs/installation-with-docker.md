# How to install Scalar DL in your local environment with Docker

This document shows how to set up a local environment that runs Scalar DL
along with the back-end Cassandra server using [Docker
Compose](https://docs.docker.com/compose/).

## Prerequisites

- [Docker Engine](https://docs.docker.com/engine/) and [Docker Compose](https://docs.docker.com/compose/).

    Follow the instructions on the Docker website according to your platform.


## Clone scalar-samples repository

[scalar-labs/scalar-samples](https://github.com/scalar-labs/scalar-samples)
repository is a project for users to start working on Scalar DL instantly.

Determine the location on your local machine where you want to run the
scalar-samples app. Then open the terminal and `cd` to the directory and run the
following commands:

```
$ git clone https://github.com/scalar-labs/scalar-samples.git
$ cd scalar-samples
```

## Docker login

`docker login` is required to start the Scalar DL Docker image. Because the
[`scalar-ledger`](https://github.com/orgs/scalar-labs/packages/container/package/scalar-ledger) repository
on GitHub Container Registry is currently private, your GitHub account needs to be set with permissions to access the container images.
Ask a person in charge to get your account ready.

```
$ docker login ghcr.io
```

## Start up Scalar DL Ledger

The following command starts up Scalar DL Ledger along with the back-end Cassandra server in the Docker containers. At the first time you run the command, the required Docker images will be downloaded from GitHub Container Registry.

```
$ docker-compose up
```

You may want to run the containers in the background. Add the `-d` (`--detach`) option:

```
$ docker-compose up -d
```

## Shut down Scalar DL Ledger

To shut down the containers:

- If you started the containers in the foreground, type `Ctrl+C` in the terminal
  where `docker-compose` is running.
- If you started the containers in the background, run the following command.

```
$ docker-compose down
```
