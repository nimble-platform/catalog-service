#!/usr/bin/env bash

set -e    # Exit immediately if a command exits with a non-zero status.


if [ "$1" == "java-build" ]; then

    mvn clean install -DskipTests

if [ "$1" == "docker-build" ]; then

    mvn -f catalogue-service-micro/pom.xml docker:build -P docker

elif [ "$1" == "docker-run" ]; then

    docker run --rm -it -e SPRING_PROFILES_ACTIVE=docker -p "8095:8095" nimbleplatform/catalogue-service-micro-srdc:latest

elif [ "$1" == "docker-run-integrated" ]; then

    # execute containers in nimble network
    docker network create nimbleinfra_default
    docker-compose -f business/catalogue-service-micro/docker-compose.yml --project-name catalogueservice up

elif [ "$1" == "docker-push" ]; then

    docker push nimbleplatform/catalogue-service-micro-srdc

fi
