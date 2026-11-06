FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY backend/shared-dto backend/shared-dto
COPY backend/api-gateway backend/api-gateway
COPY backend/room-service/pom.xml backend/room-service/pom.xml
COPY backend/game-engine/pom.xml backend/game-engine/pom.xml
COPY backend/word-service/pom.xml backend/word-service/pom.xml

RUN mvn clean package -pl backend/api-gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
