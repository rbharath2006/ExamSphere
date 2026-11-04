FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn

# Ensure the wrapper is a Unix script when the build context came from Windows.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system appuser && useradd --system --gid appuser --home-dir /app --no-create-home appuser

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
