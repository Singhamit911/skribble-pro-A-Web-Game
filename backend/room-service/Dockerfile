FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY backend/shared-dto backend/shared-dto
COPY backend/room-service backend/room-service
COPY backend/api-gateway/pom.xml backend/api-gateway/pom.xml
COPY backend/game-engine/pom.xml backend/game-engine/pom.xml
COPY backend/word-service/pom.xml backend/word-service/pom.xml

RUN mvn clean package -pl backend/room-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/room-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
