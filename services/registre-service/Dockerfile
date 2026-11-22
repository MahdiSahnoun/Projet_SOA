# ----------------------------------------------------------------
# Étape 1 — Build avec Maven
# ----------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copie du POM parent + module
COPY services/pom.xml services/pom.xml
COPY services/registre-service/pom.xml services/registre-service/pom.xml

# Téléchargement des dépendances (mise en cache)
RUN mvn -f services/registre-service/pom.xml dependency:go-offline -B

# Copie des sources
COPY services/registre-service/src services/registre-service/src

RUN mvn -f services/registre-service/pom.xml clean package -DskipTests -B

# ----------------------------------------------------------------
# Étape 2 — Image de production JRE 21
# ----------------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/services/registre-service/target/*.jar registre-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "registre-service.jar"]
