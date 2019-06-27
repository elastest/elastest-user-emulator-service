FROM node:8-slim

# Set Image Labels
ARG GIT_COMMIT=unspecified
LABEL git_commit=$GIT_COMMIT

ARG COMMIT_DATE=unspecified
LABEL commit_date=$COMMIT_DATE

ARG VERSION=unspecified
LABEL version=$VERSION

RUN npm -g install browser-sync

WORKDIR /source

COPY commands.sh /source/commands.sh

RUN chmod 777 /source/commands.sh

CMD sh -c "/source/commands.sh"
