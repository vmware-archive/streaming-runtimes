FROM maven:3.8.3-jdk-11


WORKDIR /usr/src/multibinder-grpc
COPY . /usr/src/multibinder-grpc


RUN mvn -B -DnewVersion=$(cat ./VERSION) -DgenerateBackupPoms=false versions:set
RUN mvn package
RUN mkdir -p /out && cp target/multibinder-grpc-$(cat ./VERSION).jar /out/multibinder-grpc.jar

ENTRYPOINT ["java", "-jar", "/out/multibinder-grpc.jar"]
