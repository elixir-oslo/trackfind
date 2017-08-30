FROM java:8-jre-alpine

MAINTAINER https://github.com/dtitov

COPY "target/trackfind-*.jar" trackfind.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/trackfind.jar"]
