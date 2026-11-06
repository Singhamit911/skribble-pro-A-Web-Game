FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY backend/shared-dto backend/shared-dto
COPY backend/word-service backend/word-service
COPY backend/api-gateway/pom.xml backend/api-gateway/pom.xml
COPY backend/room-service/pom.xml backend/room-service/pom.xml
COPY backend/game-engine/pom.xml backend/game-engine/pom.xml

RUN mvn clean package -pl backend/word-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/word-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
