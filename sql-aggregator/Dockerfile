FROM maven:3.8.3-jdk-11


WORKDIR /usr/src/sql-aggregator
COPY . /usr/src/sql-aggregator


RUN mvn -B -DnewVersion=$(cat ./VERSION) -DgenerateBackupPoms=false versions:set
RUN mvn package
RUN mkdir -p /out && cp target/sql-aggregator-$(cat ./VERSION).jar /out/sql-aggregator.jar

ENTRYPOINT [ "java", "-jar", "/out/sql-aggregator.jar" ]
