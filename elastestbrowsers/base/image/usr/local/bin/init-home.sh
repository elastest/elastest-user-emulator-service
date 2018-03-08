#!/bin/bash

[ -d "$DOCKER_HOME/.config" ] || mkdir $DOCKER_HOME/.config

# This makes sure that all directories in HOME are accessible by the user.
# This helps avoiding issues wiht mounted volumes.
source /etc/container_environment.sh
find $DOCKER_HOME -maxdepth 1 -type d | sed "1d" | xargs chown $DOCKER_USER:$DOCKER_GROUP 2> /dev/null || true

# It is important for $HOME/.ssh to have correct ownership
chown -R $DOCKER_USER $DOCKER_HOME/.ssh

# Initialize config directory
[ -d $DOCKER_HOME/.config/mozilla ] || cp -r /etc/X11/mozilla $DOCKER_HOME/.config/mozilla

chown -R $DOCKER_USER:$DOCKER_USER $DOCKER_HOME/.config
