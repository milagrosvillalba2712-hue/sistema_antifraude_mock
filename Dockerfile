FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode clean package

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 regula \
    && mkdir -p /var/lib/regula-mock/audit \
    && chown -R regula:regula /var/lib/regula-mock
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER regula
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
