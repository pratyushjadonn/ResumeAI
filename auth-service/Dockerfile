# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml .

# Copy module poms
COPY auth-service/pom.xml auth-service/
COPY common-events/pom.xml common-events/

# Download dependencies
RUN mvn dependency:go-offline -pl auth-service -am

# Copy source code
COPY auth-service/src auth-service/src
COPY common-events/src common-events/src

# Build auth-service
RUN mvn clean package -DskipTests -pl auth-service -am

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy generated jar
COPY --from=build /app/auth-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar app.jar"]