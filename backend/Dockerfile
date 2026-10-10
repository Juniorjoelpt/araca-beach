# ---- Etapa 1: build do jar com Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia so o pom.xml primeiro para aproveitar o cache de dependencias
# em builds futuros (so reprocessa se o pom.xml mudar)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Etapa 2: imagem final, so com o jar e o Java (sem o Maven) ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# Horario de Brasilia (Fortaleza, UTC-3): datas/horas do caixa, relatorios e agendadores dependem disso.
ENV TZ=America/Fortaleza
ENTRYPOINT ["java", "-Duser.timezone=America/Fortaleza", "-jar", "app.jar"]
