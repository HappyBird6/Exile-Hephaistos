FROM eclipse-temurin:21-jdk-jammy@sha256:c7d5863b5dd8f26b90c64f1d80cc2b0e5a5e4642f8db9955a370d348edd8f438 AS build
WORKDIR /workspace
COPY backend/ ./
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar
FROM python:3.12-slim@sha256:2f17fc044b579bab302c2e8054d3a686e2cb9a83de48e70534b94cd8ebbe06a9
COPY --from=build /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"
ENV PYTHONPATH=/app/python
ENV APP_CRAWL_DIRECTORY=/app/captures
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
RUN mkdir -p /app/captures && chown app:app /app/captures && java -version && python --version
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/poe2craft.jar ./app.jar
COPY --chown=app:app data-pipeline/src/ /app/python/
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
