# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Etapa 1: build do frontend (React + Vite)
# ---------------------------------------------------------------------------
FROM node:22-alpine AS frontend
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ---------------------------------------------------------------------------
# Etapa 2: build do backend (Spring Boot + Maven Wrapper)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# O build do frontend foi emitido em /app/src/main/resources/static (vite outDir).
COPY --from=frontend /app/src/main/resources/static/ src/main/resources/static/

RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# ---------------------------------------------------------------------------
# Etapa 3: imagem final (somente JRE + JAR executável)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/sintonia-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
