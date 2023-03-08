FROM openjdk:11
LABEL maintainer="rschiemann@parkerici.org"

EXPOSE 8989

WORKDIR /sample-tracking

COPY resources resources

COPY target/sample-tracking-standalone.jar .

# $PORT didn't work
# get config from kub env vars
ENTRYPOINT ["java", "-jar", "sample-tracking-standalone.jar", "server", "-p", "8989"]
