# Контекст сборки — корень репозитория (см. docker-compose.yml)
FROM gradle:9.7.1-jdk25-ubi AS build
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN gradle :service:buildFatJar --no-daemon -PjavaToolchain=25

FROM eclipse-temurin:25-jre
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/service/build/libs/*-all.jar /app/application.jar
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
