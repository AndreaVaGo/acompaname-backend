# Imagen del back de Acompáñame (Spring Boot). Se construye en dos fases:
# 1) se compila con Maven y 2) se copia solo el .jar a una imagen pequeña que solo tiene Java.
# Así la imagen final no lleva Maven, ni el código fuente, ni las dependencias de compilación.

# ---- Fase 1: compilar ----
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app

# Primero solo el pom: mientras no cambien las dependencias, Docker reutiliza esta capa y no las baja otra vez
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Los tests se pasan antes, en local. Aquí solo se empaqueta
RUN mvn -B -q -DskipTests package

# ---- Fase 2: ejecutar ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# La aplicación no se ejecuta como administrador del contenedor
RUN addgroup -S acompaname && adduser -S acompaname -G acompaname
COPY --from=compilar --chown=acompaname:acompaname /app/target/*.jar app.jar
USER acompaname

# La imagen arranca SIEMPRE con el perfil de producción: no tiene claves por defecto y, si falta
# alguna variable de entorno (DB_USER, DB_PASSWORD, JWT_SECRET...), la aplicación no arranca.
ENV SPRING_PROFILES_ACTIVE=prod
# Memoria: el 75 % de la que tenga el contenedor. Zona horaria: la de España, para las fechas y la limpieza de las 03:30
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Europe/Madrid"

EXPOSE 8080

# "liveness" no mira la base de datos: si MySQL se cae, Docker no reinicia la aplicación en bucle
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/actuator/health/liveness || exit 1

# exec: Java pasa a ser el proceso principal y recibe la señal de parada de Docker (cierre limpio)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
