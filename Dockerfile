FROM java:8-jre-alpine

ADD "target/trackfind-*.jar" trackfind.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/trackfind.jar"]
