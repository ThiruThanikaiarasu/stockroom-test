# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY stockroom-core/pom.xml stockroom-core/
COPY stockroom-reporting/pom.xml stockroom-reporting/
COPY stockroom-api/pom.xml stockroom-api/
RUN ./mvnw -B -q dependency:go-offline -DexcludeArtifactIds=stockroom-core,stockroom-reporting
COPY stockroom-core/src stockroom-core/src
COPY stockroom-reporting/src stockroom-reporting/src
COPY stockroom-api/src stockroom-api/src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 stockroom
WORKDIR /app
COPY --from=build /workspace/stockroom-api/target/stockroom-api-*.jar app.jar
RUN mkdir /app/data && chown stockroom /app/data
USER stockroom
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
