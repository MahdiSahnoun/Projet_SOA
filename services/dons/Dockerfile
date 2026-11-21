# Image du service Dons — à construire depuis la racine du dépôt :
#   docker build -f dons/Dockerfile .
# Étape 1 : compilation Maven du module et de ses contrats
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl dons -am package -DskipTests

# Étape 2 : image d'exécution légère, utilisateur non root
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/dons/target/dons-*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=12 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
