# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml ./
COPY auth-service/pom.xml ./auth-service/

# Download dependencies
RUN mvn dependency:go-offline -pl auth-service -am

# Copy source
COPY auth-service/src ./auth-service/src

# Build
RUN mvn clean package -DskipTests -pl auth-service -am

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/auth-service-1.0.0.jar app.jar
EXPOSE 8081
CMD ["java", "-Dserver.port=${PORT:-8081}", "-jar", "app.jar"]