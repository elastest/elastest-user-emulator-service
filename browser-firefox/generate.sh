#!/bin/bash
VERSION=$1

echo "###########################################" > ./Dockerfile
echo "# Generated file with script (generate.sh)" >> ./Dockerfile
echo "# Do not commit these changes!" >> ./Dockerfile
echo "###########################################" >> ./Dockerfile

echo FROM selenium/standalone-firefox-debug:$VERSION >> ./Dockerfile
echo USER root >> ./Dockerfile
echo RUN apt-get update && apt-get install -y pulseaudio >> ./Dockerfile
echo USER seluser >> ./Dockerfile
