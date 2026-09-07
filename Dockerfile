# ---- Build & test stage ----
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /src

COPY order-api/ ./order-api/
WORKDIR /src/order-api

# `mvn verify` runs unit tests (Surefire, *Tests.java) AND integration
# tests (Failsafe, *IT.java) in sequence, then packages the jar. A failure
# at either layer stops the image from ever being produced -- the same
# fail-the-build-on-real-findings pattern as npm-scan-demo's `npm audit`
# gate and the .NET version's `dotnet test` gate, just enforced via Maven
# here. Functional/E2E tests are NOT run here; they need a real deployed
# instance to test against -- see Dockerfile.functional-tests and the README.
RUN mvn -B verify

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /src/order-api/target/order-api.jar app.jar

# OpenShift runs containers under an arbitrary, randomly-assigned non-root
# UID (with group 0). This app writes nothing to disk at runtime, so no
# extra permission changes are needed for that under OpenShift's
# restricted SCC.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
