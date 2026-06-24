FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/app.jar app.jar

EXPOSE 3002

ENTRYPOINT ["java", "-jar", "app.jar"]
