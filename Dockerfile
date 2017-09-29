FROM ubuntu:17.04

MAINTAINER https://github.com/dtitov

RUN mkdir /cluster

RUN apt update && apt install git openjdk-8-jre -yq --no-install-recommends

ADD https://github.com/git-lfs/git-lfs/releases/download/v2.3.0/git-lfs-linux-amd64-2.3.0.tar.gz lfs/
RUN tar -xvzf lfs/git-lfs-linux-amd64-2.3.0.tar.gz && git-lfs-2.3.0/install.sh && git lfs install

EXPOSE 8888

COPY target/trackfind-*.jar /trackfind/trackfind.jar
WORKDIR trackfind

ENTRYPOINT ["java", "-jar", "trackfind.jar"]
