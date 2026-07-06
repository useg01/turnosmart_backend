# 1. Etapa de compilación (Build)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# Copiar el pom y descargar dependencias (para aprovechar la caché de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline
# Copiar el código fuente y compilar el archivo .jar saltando los tests
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Etapa de ejecución (Runtime)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copiar el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]