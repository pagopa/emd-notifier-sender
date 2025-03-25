#
# Build
#
FROM maven:3.9.6-amazoncorretto-17-al2023@sha256:459be099faa25a32c06cd45ed1ef2bc9dbbf8a5414da4e72349459a1bb4d6166 AS buildtime

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17-alpine3.19@sha256:539a0a188ce5a2bed985aa311e9a26d473c6c3f37d08d4fc8b6cf6c18075b9ab AS runtime

RUN apk add --no-cache shadow vim curl net-tools bind-tools netcat-openbsd wget

RUN useradd --uid 10000 runner

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.1/applicationinsights-agent-3.7.1.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
