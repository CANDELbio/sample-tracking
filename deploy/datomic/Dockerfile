FROM naartjie/alpine-lein

# Based on pointslope/datomic-pro-starter
# https://hub.docker.com/r/pointslope/datomic-pro-starter/dockerfile

MAINTAINER Mike Travers "mtravers@parkerici.org"

# Set the version to the one you're downloading.
ENV DATOMIC_VERSION=1.0.6202
ENV DATOMIC_HOME /opt/datomic-pro-$DATOMIC_VERSION
ENV DATOMIC_DATA $DATOMIC_HOME/data

RUN apk add --no-cache unzip curl

# Datomic Pro Starter as easy as 1-2-3
# 1. Download a release from my.datomic.com and put it in the releases folder
ADD releases/datomic-pro-1.0.6202.zip /tmp/datomic.zip

RUN  unzip /tmp/datomic.zip -d /opt \
  && rm -f /tmp/datomic.zip

WORKDIR $DATOMIC_HOME
RUN echo DATOMIC HOME: $DATOMIC_HOME

# 3. Provide a CMD argument with the relative path to the
# transactor.properties file it will supplement the ENTRYPOINT
VOLUME $DATOMIC_DATA

EXPOSE 4334 4335 4336