FROM node:8-slim

RUN npm -g install browser-sync

WORKDIR /source

COPY commands.sh /source/commands.sh

RUN chmod 777 /source/commands.sh

CMD sh -c "/source/commands.sh"
