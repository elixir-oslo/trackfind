FROM openjdk:8-jre

MAINTAINER https://github.com/dtitov

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl ca-certificates && \
    curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git-lfs && \
    git lfs install && \
    rm -r /var/lib/apt/lists/*

COPY target/trackfind-*.jar /trackfind/trackfind.jar

WORKDIR trackfind

ENTRYPOINT ["java", "-jar", "trackfind.jar", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap"]
