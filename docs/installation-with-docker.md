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
[`scalar-server`](https://hub.docker.com/r/scalarlabs/scalar-server/) repository
on Docker Hub is currently private, your Docker account needs to be listed as a
collaborator of the repository. Ask a person in charge to get your account
ready.

```
$ docker login
```

## Start up the server

The following command starts up the Scalar DL server along with the back-end
Cassandra server in the Docker containers. At the first time you run the
command, the required Docker images will be downloaded from Docker Hub.

```
$ docker-compose up
```

You may want to run the containers in the background. Add the `-d` (`--detach`) option:

```
$ docker-compose up -d
```

## Shut down the server

To shut down the containers:

- If you started the containers in the foreground, type `Ctrl+C` in the terminal
  where `docker-compose` is running.
- If you started the containers in the background, run the following command.

```
$ docker-compose down
```
