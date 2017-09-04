FROM java:8-jre-alpine

MAINTAINER https://github.com/dtitov

RUN mkdir /cluster

RUN mkdir /trackfind

COPY target/trackfind-*.jar /trackfind/trackfind.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/trackfind/trackfind.jar"]