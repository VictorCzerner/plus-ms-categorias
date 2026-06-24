FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/build/libs/app.jar app.jar

EXPOSE 3002

ENTRYPOINT ["java", "-jar", "app.jar"]
