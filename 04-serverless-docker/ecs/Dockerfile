# Stage 1: build the jar inside Docker, so no local Maven is needed to build the image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Stage 2: small runtime image.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/students-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
