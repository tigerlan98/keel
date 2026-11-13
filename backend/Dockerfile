FROM eclipse-temurin:25-jre

ENV TZ=Asia/Shanghai
WORKDIR /app

RUN groupadd --system keel && useradd --system --gid keel --home-dir /app keel

COPY keel-bootstrap/target/keel-bootstrap-1.0.0.jar app.jar

USER keel
EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
