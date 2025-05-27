# Etapa de Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de Ejecución
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
RUN groupadd --gid 1001 appgroup && useradd --uid 1001 --gid 1001 --shell /bin/sh --create-home appuser
COPY --from=build /app/target/sales-point-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]