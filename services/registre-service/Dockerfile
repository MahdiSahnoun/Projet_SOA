# Contexte de build : racine du dépôt
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/pom.xml -B -q -pl registre-service -am package -DskipTests

# Étape 2 — Image d'exécution légère, utilisateur non root
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/services/registre-service/target/registre-service-*.jar registre-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/registre-service.jar"]
