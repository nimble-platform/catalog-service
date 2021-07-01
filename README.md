# Catalog Service
**ServiceID**: catalog-service

Microservice providing functionalities to manage products and catalogues of products in Nimble.

## How to build

The project can be build using the following command:
 ```bash
 mvn clean install
 ```
The command above builds the projects and runs tests. You can skip the tests by executing the command below.
 ```bash
 mvn clean install -DskipTests
 ```
## Getting started

Catalogue Service is one of the main modules of Nimble project. It is built on the top of data model presented in the 
[common module](https://github.com/nimble-platform/common). Before using Catalogue Service, you have to clone common 
repository and build the data model using the following command:
 ```bash
 mvn clean install
 ```

Here are the two services used by catalogue service:

* [Indexing service](https://github.com/nimble-platform/indexing-service) : The service which indexes the metadata of products / services and provides search
functionality for them.
* [Identity service](https://github.com/nimble-platform/identity-service) : The service which handles the authentication of users and provides company
and user information.

Further, catalogue service makes use of Postgresql as the relational database management system to store product and catalogue
information.

## Run Locally

You can run the application using the following command:
 ```
mvn spring-boot:run -DUBL_DB_HOST=localhost -DUBL_DB_HOST_PORT=5432 -DVARIABLE_NAME=VALUE ...
 ```
Note that you need to pass some environment variables to make it running. You can pass them one by one, or you can simply
set their values in [bootstrap.yml](./catalogue-service/src/main/resources/bootstrap.yml).

## Configuration

Catalogue service configuration is achieved via [bootstrap.yml](./catalogue-service/src/main/resources/bootstrap.yml).
It has some environment variables to configure database connection, available taxonomies and some functionalities.

## Swagger

Swagger is used to present REST endpoints provided by catalogue service to manage products and catalogues of products.

Swagger UI can be accessed via http://localhost:8095/swagger-ui.html

## Docker

You can create a docker image of Catalogue Service and run it in Docker.

First of all, you need to build project:
```
mvn clean install -DskipTests
```

Build the docker image by setting its tag via '-Ddocker.image.tag' option:
```
mvn -f catalogue-service-micro/pom.xml docker:build -Ddocker.image.tag=<TAG_HERE> -P docker
```

Finally, run the docker image by passing all the required environment variables (they are
passed from a file using '--env-file' parameter'):
```
docker run --env-file <PATH_TO_VARIABLES_FILE> --rm -it -e SPRING_PROFILES_ACTIVE=docker -p "8095:8095" 
nimbleplatform/catalogue-service-micro:<TAG_HERE> 
```

You can also push the image to a registry:
```
docker push nimbleplatform/catalogue-service-micro:<TAG_HERE>
```

## Docker Compose

You can also run catalogue service via [docker-compose](./catalogue-service-micro/docker-compose.yml).
```
docker compose up -d
```

## Language and IDE
Java (1.8) is used to build and run this application. Any IDE which allows development in Java can be used.