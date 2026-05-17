# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml .

# Copy module poms required for resume-service build
COPY resume-service/pom.xml resume-service/
COPY common-events/pom.xml common-events/

# Download dependencies for resume-service and its required module
RUN mvn dependency:go-offline -pl resume-service -am

# Copy source code
COPY resume-service/src resume-service/src
COPY common-events/src common-events/src

# Build resume-service
RUN mvn clean package -DskipTests -pl resume-service -am

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy generated jar
COPY --from=build /app/resume-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8082} -jar app.jar"]
