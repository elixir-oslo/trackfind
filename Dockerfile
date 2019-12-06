FROM maven:3.6.0-jdk-8-alpine as builder
COPY . .
# Here we skip tests to save time, because if this image is being built - tests have already passed...
RUN mvn install -DskipTests

FROM openjdk:8-jre-alpine

COPY --from=builder /target/trackfind-*.jar trackfind.jar

CMD ["java", "-jar", "trackfind.jar"]
