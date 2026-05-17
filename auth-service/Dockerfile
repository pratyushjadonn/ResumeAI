# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml .

# Copy auth-service pom
COPY auth-service/pom.xml auth-service/

# Download dependencies for auth-service only
RUN mvn -f auth-service/pom.xml dependency:go-offline

# Copy source code
COPY auth-service/src auth-service/src

# Build auth-service
RUN mvn -f auth-service/pom.xml clean package -DskipTests

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy generated jar
COPY --from=build /app/auth-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar app.jar"]
