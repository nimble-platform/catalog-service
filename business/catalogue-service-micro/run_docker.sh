#!/usr/bin/env bash

# create docker image
mvn docker:build -P docker

# run docker image
docker run --rm -it nimbleplatform/catalogue-service-micro-srdc