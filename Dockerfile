FROM openjdk:8-jdk-alpine

COPY target/trackfind-*.jar trackfind.jar

CMD ["java", "-jar", "trackfind.jar", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap"]
