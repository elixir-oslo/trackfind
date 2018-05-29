FROM ubuntu:18.04

MAINTAINER https://github.com/dtitov

RUN apt update && apt install git openjdk-8-jre -yq --no-install-recommends

RUN build_deps="curl ca-certificates" && \
    apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ${build_deps} && \
    curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git-lfs && \
    git lfs install && \
    DEBIAN_FRONTEND=noninteractive apt-get purge -y --auto-remove ${build_deps} && \
    rm -r /var/lib/apt/lists/*

COPY target/trackfind-*.jar /trackfind/trackfind.jar

WORKDIR trackfind

ENTRYPOINT ["java", "-jar", "trackfind.jar", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap"]
