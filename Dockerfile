FROM eclipse-temurin:17-jdk-alpine

WORKDIR /work/
COPY target/quarkus-app/ /work/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]







