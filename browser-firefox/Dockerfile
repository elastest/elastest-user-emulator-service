FROM selenium/standalone-firefox-debug

# Set Image Labels
ARG GIT_COMMIT=unspecified
LABEL git_commit=$GIT_COMMIT

ARG COMMIT_DATE=unspecified
LABEL commit_date=$COMMIT_DATE

ARG VERSION=unspecified
LABEL version=$VERSION 

USER root

RUN apt-get update && apt-get install -y pulseaudio

USER seluser
