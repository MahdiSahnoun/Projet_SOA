# ----------------------------------------------------------------
# Image du service Registre (SOAP, port 8083)
# Contexte de build = services/ :
#   docker build -f registre-service/Dockerfile -t registre-service services/
# ----------------------------------------------------------------

# Étape 1 — Build Maven : POM parent + contrat SOAP partagé + Registre
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# POM parent. Maven lit tous les modules déclarés dans <modules> pour construire
# le réacteur : les modules non construits n'ont besoin que de leur pom.xml.
COPY pom.xml pom.xml
COPY contrats/inventaire-grpc/pom.xml contrats/inventaire-grpc/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY discovery/pom.xml discovery/pom.xml
COPY dons/pom.xml dons/pom.xml

# Modules réellement construits : le contrat (XSD + classes JAXB) et le Registre
COPY contrats/registre-soap contrats/registre-soap
COPY registre-service registre-service

# -am construit aussi contrats/registre-soap ; le cache garde le dépôt Maven entre deux builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl registre-service -am package -DskipTests

# Étape 2 — Image d'exécution légère, utilisateur non root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/registre-service/target/registre-service-*.jar registre-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/registre-service.jar"]
