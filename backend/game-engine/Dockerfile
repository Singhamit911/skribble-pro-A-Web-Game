FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY backend/shared-dto backend/shared-dto
COPY backend/game-engine backend/game-engine
COPY backend/api-gateway/pom.xml backend/api-gateway/pom.xml
COPY backend/room-service/pom.xml backend/room-service/pom.xml
COPY backend/word-service/pom.xml backend/word-service/pom.xml

RUN mvn clean package -pl backend/game-engine -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/game-engine/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
