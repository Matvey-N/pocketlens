# Build stage: compiles and packages the Spring Boot application (tests run in CI before this image is built).
# Build context is the repository root, because the OpenAPI contract lives in docs/.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -B -q dependency:go-offline
COPY backend/src backend/src
COPY docs/reference docs/reference
RUN mvn -f backend/pom.xml -B -q -DskipTests package && \
    java -Djarmode=tools -jar backend/target/pocketlens.jar extract --layers --launcher --destination /workspace/extracted

# Runtime stage: small JRE image, non-root user, layered jar for fast rebuilds.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
