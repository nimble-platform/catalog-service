FROM nimbleplatform/nimble-base
MAINTAINER Salzburg Research <nimble-srfg@salzburgresearch.at>
VOLUME /tmp
ARG finalName
ENV JAR '/app.jar
ARG port
ADD $finalName $JAR
RUN touch $JAR
ENV PORT 8095

EXPOSE $PORT

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]