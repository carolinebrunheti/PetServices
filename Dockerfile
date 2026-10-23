# estagio 1: compila o projeto com o JDK
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# baixa as dependencias numa camada separada: so refaz quando o pom.xml muda
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

# estagio 2: imagem final so com o JRE e o jar (menor e sem ferramentas de build)
FROM eclipse-temurin:17-jre
WORKDIR /app
# roda sem root: se alguem invadir o container, nao tem privilegio de administrador
RUN useradd --system --uid 10001 app
COPY --from=build /app/target/*.jar app.jar
USER 10001
EXPOSE 8081
# o JVM usa ate 75% da memoria limite que o Kubernetes der ao container
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
