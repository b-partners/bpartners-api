FROM amazoncorretto:11-alpine
ARG version
ARG JAR_FILE=build/libs/bpartners-api-$version.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djdk.httpclient.HttpClient.log=all","-Djdk.httpclient.keepalive.timeout=2","-Djdk.httpclient.connectionPoolSize=1","-jar","/app.jar"]
EXPOSE 8080
