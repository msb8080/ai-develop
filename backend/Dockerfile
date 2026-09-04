FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B package

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 rainbow
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER rainbow
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/app.jar"]
