#!/bin/bash
VERSION=$1

echo "###########################################" > ./Dockerfile
echo "# Generated file with script (generate.sh)" >> ./Dockerfile
echo "# Do not commit these changes!" >> ./Dockerfile
echo "###########################################" >> ./Dockerfile

echo FROM selenium/standalone-firefox-debug:$VERSION >> ./Dockerfile
echo ARG GIT_COMMIT=unspecified >> ./Dockerfile
echo "LABEL git_commit=$GIT_COMMIT" >> ./Dockerfile
echo "ARG COMMIT_DATE=unspecified" >> ./Dockerfile
echo "LABEL commit_date=$COMMIT_DATE" >> ./Dockerfile
echo "ARG VERSION=unspecified" >> ./Dockerfile
echo LABEL version=$VERSION >> ./Dockerfile 
echo "USER root" >> ./Dockerfile
echo "RUN apt-get update && apt-get install -y pulseaudio" >> ./Dockerfile
echo "USER seluser" >> ./Dockerfile
