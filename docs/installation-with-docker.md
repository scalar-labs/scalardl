> [!ATTENTION]
> 
> The `docs` folder has been moved to the centralized documentation repository, [docs-internal](https://github.com/scalar-labs/docs-internal). Please update this documentation in that repository instead.

# How to install ScalarDL in your local environment with Docker

This document shows how to set up a local environment that runs ScalarDL
along with the back-end Cassandra server using [Docker
Compose](https://docs.docker.com/compose/).

## Prerequisites

- [Docker Engine](https://docs.docker.com/engine/) and [Docker Compose](https://docs.docker.com/compose/).

    Follow the instructions on the Docker website according to your platform.


## Clone scalardl-samples repository

[scalar-labs/scalardl-samples](https://github.com/scalar-labs/scalardl-samples)
repository is a project for users to start working on ScalarDL instantly.

Determine the location on your local machine where you want to run the
scalardl-samples app. Then open the terminal and `cd` to the directory and run the
following commands:

```
$ git clone https://github.com/scalar-labs/scalardl-samples.git
$ cd scalardl-samples
```

## Docker login

`docker login` is required to start the ScalarDL Docker image. Because the
[`scalar-ledger`](https://github.com/orgs/scalar-labs/packages/container/package/scalar-ledger) repository
on GitHub Container Registry is currently private, your GitHub account needs to be set with permissions to access the container images.
Ask a person in charge to get your account ready. Note also that you need to use a personal access token (PAT) as a password to login `ghcr.io`. Please read [the official document](https://docs.github.com/en/packages/guides/migrating-to-github-container-registry-for-docker-images#authenticating-with-the-container-registry) for more detail.

```
# read:packages scope needs to be selected in a personal access token to login
$ export CR_PAT=YOUR_PERSONAL_ACCESS_TOKEN
$ echo $CR_PAT | docker login ghcr.io -u USERNAME --password-stdin
```

## Start up ScalarDL Ledger

The following command starts up ScalarDL Ledger along with the back-end Cassandra server in the Docker containers. At the first time you run the command, the required Docker images will be downloaded from GitHub Container Registry.

```
$ docker-compose up
```

You may want to run the containers in the background. Add the `-d` (`--detach`) option:

```
$ docker-compose up -d
```

When you want to use Auditor component, run the following command.
```
$ docker-compose -f docker-compose.yml -f docker-compose-auditor.yml up -d

```

## Shut down ScalarDL Ledger

To shut down the containers:

- If you started the containers in the foreground, type `Ctrl+C` in the terminal
  where `docker-compose` is running.
- If you started the containers in the background, run the following command.

```
$ docker-compose down

# When Auditor is running
$ docker-compose -f docker-compose.yml -f docker-compose-auditor.yml down
```
