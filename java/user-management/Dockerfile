FROM openjdk:11-oracle
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
#RUN echo "Main-Class: dev.sunbirdrc.UserManagementApplication" > MANIFEST.MF
#RUN jar cvfm app.jar MANIFEST.MF
ENTRYPOINT ["java","-jar","/app.jar"]
