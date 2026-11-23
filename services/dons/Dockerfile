# Contexte de build : racine du dépôt
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY services ./services
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/pom.xml -q -B -pl dons -am package -DskipTests

# Étape 2 : image d'exécution légère, utilisateur non root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/services/dons/target/dons-*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=12 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
