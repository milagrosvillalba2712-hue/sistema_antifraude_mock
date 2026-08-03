FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY scripts ./scripts
COPY openapi ./openapi
RUN javac -d /health scripts/HealthCheck.java
RUN mvn --batch-mode clean package

FROM eclipse-temurin:17-jre
RUN mkdir -p /var/lib/regula-mock/audit /app/health \
    && chown -R 10001:10001 /var/lib/regula-mock /app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
COPY --from=build /health /app/health
USER 10001:10001
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
