FROM openjdk:8-jre-alpine

COPY target/trackfind-*.jar trackfind.jar

CMD ["java", "-jar", "trackfind.jar", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap"]
