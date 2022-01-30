FROM amazoncorretto:17

ENV TZ="America/Denver"
ADD . /nftservice
WORKDIR nftservice
RUN ./gradlew bootJar

CMD java -jar /nftservice/build/libs/nftservice-1.0-SNAPSHOT.jar
