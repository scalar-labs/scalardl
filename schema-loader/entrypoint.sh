#!/usr/bin/env sh
set -e

dockerize -template database.properties.tmpl:/tmp/database.properties java -jar /app.jar -f ${SCHEMA_TYPE}-schema.json $@
