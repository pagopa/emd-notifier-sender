#
# Build
#
FROM maven:3.9.6-amazoncorretto-21-al2023@sha256:8d653ed25358201bdb352ce0d24e4be2f1e34ddf372d3381d22876f9c483cfa1 AS buildtime

WORKDIR /build

COPY pom.xml .
COPY src ./src
COPY dep-sha256.json .

# Setting ENV variables for GITHUB_TOKEN
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# settings.xml file creation with GitHub token
RUN echo '<?xml version="1.0" encoding="UTF-8"?>' > settings.xml && \
    echo '<settings>' >> settings.xml && \
    echo '  <servers>' >> settings.xml && \
    echo '    <server>' >> settings.xml && \
    echo '      <id>github</id>' >> settings.xml && \
    echo '      <username></username>' >> settings.xml && \
    echo "      <password>${GITHUB_TOKEN}</password>" >> settings.xml && \
    echo '    </server>' >> settings.xml && \
    echo '  </servers>' >> settings.xml && \
    echo '</settings>' >> settings.xml


# Maven build with settings.xml file
RUN mvn --global-settings settings.xml clean package -DskipTests && rm settings.xml

#
# Docker RUNTIME
#
FROM amazoncorretto:21-alpine3.20@sha256:d35e44aa90121164411d3f647e116d6c4c42461ba67dabc7c5ca6e460d380c12 AS runtime

RUN apk add --no-cache shadow vim curl net-tools bind-tools netcat-openbsd wget

RUN useradd --uid 10000 runner

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.6.1/applicationinsights-agent-3.6.1.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
