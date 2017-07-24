FROM java:8-jre-alpine

ADD "target/trackfind-0.0.1-SNAPSHOT.jar" trackfind.jar

EXPOSE 8080

ENTRYPOINT ["/usr/bin/java", "-jar", "/trackfind.jar"]
