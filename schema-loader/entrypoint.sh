#!/usr/bin/env sh
set -e

java -jar /app.jar -f ${SCHEMA_TYPE}-schema.json $@
