# Contexte de build : racine du dépôt
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/pom.xml -B -q -pl inventaire-service -am package -DskipTests

# Étape 2 — Image d'exécution légère, utilisateur non root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/services/inventaire-service/target/inventaire-service-*.jar inventaire-service.jar
# 8080 : HTTP (actuator), 9090 : gRPC
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "/app/inventaire-service.jar"]
