FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY services/discovery/pom.xml services/discovery/pom.xml
COPY services/discovery/src services/discovery/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f services/discovery/pom.xml -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /src/services/discovery/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
