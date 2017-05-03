#!/usr/bin/env bash

# build project
mvn clean install

# create docker image
mvn -f business/catalogue-service-micro/pom.xml docker:build -P docker

# execute containers in nimble network
docker network create nimbleinfra_default
docker-compose -f business/catalogue-service-micro/docker-compose.yml --project-name catalogueservice up