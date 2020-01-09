FROM maven:3.6.1-jdk-8-alpine as builder

COPY pom.xml .

RUN mvn dependency:go-offline --no-transfer-progress

COPY src/ /src/

# Here we skip tests to save time, because if this image is being built - tests have already passed...
RUN mvn install -DskipTests --no-transfer-progress

FROM openjdk:8-jre-alpine

COPY --from=builder /target/trackfind-*.jar trackfind.jar

CMD ["java", "-jar", "trackfind.jar"]
