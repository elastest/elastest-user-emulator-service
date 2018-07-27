FROM openjdk:8-jre-alpine

COPY eus/target/user-emulator-service-0.9.1-SNAPSHOT.jar /eus.jar

# Set Image Labels
ARG GIT_COMMIT=unspecified
LABEL git_commit=$GIT_COMMIT

ARG COMMIT_DATE=unspecified
LABEL commit_date=$COMMIT_DATE

ARG VERSION=unspecified
LABEL version=$VERSION 

CMD exec java -jar /eus.jar --spring.profiles.active=required,notdependency
