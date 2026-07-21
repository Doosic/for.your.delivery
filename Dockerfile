FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""

COPY delivery.jar app.jar

EXPOSE 30100

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
