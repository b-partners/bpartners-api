FROM amazoncorretto:17.0.0-alpine
ARG version
ARG JAR_FILE=build/libs/bpartners-api-$version.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
