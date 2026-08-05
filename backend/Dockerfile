# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=container
ENV ARCHITECTAI_RUNTIME_WORKSPACE_DIR=/data/workspace
ENV ARCHITECTAI_RUNTIME_REPORTS_DIR=/data/reports

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd -r -u 1001 architectai \
	&& mkdir -p /data/workspace /data/reports \
	&& chown -R architectai:architectai /data

COPY --from=build /workspace/target/backend-0.1.0-SNAPSHOT.jar /app/app.jar

USER architectai
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

