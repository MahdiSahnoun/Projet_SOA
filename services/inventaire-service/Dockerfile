FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY services/pom.xml services/pom.xml
COPY services/contrats/inventaire-grpc/pom.xml services/contrats/inventaire-grpc/pom.xml
COPY services/contrats/inventaire-grpc/src services/contrats/inventaire-grpc/src
COPY services/inventaire-service/pom.xml services/inventaire-service/pom.xml
COPY services/inventaire-service/src services/inventaire-service/src

RUN mvn -f services/pom.xml -pl inventaire-service -am clean package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/services/inventaire-service/target/*.jar inventaire-service.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "inventaire-service.jar"]
