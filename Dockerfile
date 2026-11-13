# Etapa 1: Build com Maven e Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagem final leve para produção
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /app/uploads /app/data /app/logs
COPY --from=build /app/target/plataforma-mov-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENV APP_UPLOAD_DIR=/app/uploads

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
