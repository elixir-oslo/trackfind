FROM java:8-jre-alpine

MAINTAINER The Docker Community

ADD "target/trackfind-*.jar" trackfind.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/trackfind.jar"]
