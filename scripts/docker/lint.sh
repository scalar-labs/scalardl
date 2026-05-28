#!/usr/bin/env bash

set -e -o pipefail; [[ -n "$DEBUG" ]] && set -x

SCRIPT_ROOT="$(cd "$(dirname "$0")"; pwd)"

DOCKERFILE_NAME=$1
HADOLINT_VERSION="v2.12.0"
HADOLINT="${HADOLINT:-docker run --rm -i -v "$(pwd):/mnt" -w "/mnt" "hadolint/hadolint:${HADOLINT_VERSION}" hadolint}"

exec ${HADOLINT} ./${DOCKERFILE_NAME:-Dockerfile}
# vim: ai ts=2 sw=2 et sts=2 ft=sh
