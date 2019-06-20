FROM openjdk:10
ADD target/spring-boot-hello-world-1.0-SNAPSHOT.jar spring-boot-hello-world.jar
EXPOSE 8081
ENTRYPOINT exec java -jar spring-boot-hello-world.jar