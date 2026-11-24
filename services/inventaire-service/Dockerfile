FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /src
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/pom.xml -pl inventaire-service -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /src/services/inventaire-service/target/*.jar inventaire-service.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "inventaire-service.jar"]
