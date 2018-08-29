#!/bin/bash

# Start up VNC server and launch xsession and novnc

# Author: Xiangmin Jiao <xmjiao@gmail.com>

# Copyright Xiangmin Jiao 2017. All rights reserved.

# Disable IPv6 to avoid ERR_NETWORK_CHANGED problems
sysctl -w net.ipv6.conf.all.disable_ipv6=1
sysctl -p

# Start up xdummy with the given size
RESOLUT="${RESOLUT:-1440x900}"
SIZE=`echo $RESOLUT | sed -e "s/x/ /"`
grep -s -q $RESOLUT /etc/X11/xorg.conf && \
sudo perl -i -p -e "s/Virtual \d+ \d+/Virtual $SIZE/" /etc/X11/xorg.conf

# Adjusting permissions on $HOME
/usr/local/bin/init-home.sh || true

Xorg -noreset -logfile $DOCKER_HOME/.log/Xorg.log -config /etc/X11/xorg.conf :0 2> $DOCKER_HOME/.log/Xorg_err.log &
sleep 0.1

# startup lxsession with proper environment variables
export DISPLAY=:0.0
export HOME=$DOCKER_HOME
export SHELL=$DOCKER_SHELL
export USER=$DOCKER_USER
export LOGFILE=$DOCKER_USER

eval `ssh-agent` > /dev/null

#/usr/bin/lxsession -s LXDE -e LXDE > $DOCKER_HOME/.log/lxsession.log 2>&1 &
/home/ubuntu/.fluxbox/startup &

# startup x11vnc with a new password
#export VNCPASS=`openssl rand -base64 6 | sed 's/\//-/'`
export VNCPASS=selenoid

mkdir -p $DOCKER_HOME/.vnc && \
x11vnc -storepasswd $VNCPASS ~/.vnc/passwd > $DOCKER_HOME/.log/x11vnc.log 2>&1

# Do not use -repeat option to enable keyboard repeat.
# The user can use “xset r on" twice to re-enable it.
export X11VNC_IDLE_TIMEOUT=2147483647
x11vnc -display :0 -xkb -norepeat 2 -forever -shared  -usepw >> $DOCKER_HOME/.log/x11vnc.log 2>&1 &

sudo service dbus start > $DOCKER_HOME/.log/dbus.log 2>&1

# start pulseaudio
pulseaudio -D

echo "Open your web browser with URL:"
echo "    http://localhost:6080/vnc.html?resize=downscale&autoconnect=1&password=$VNCPASS"

# startup novnc
/usr/local/noVNC/utils/launch.sh --web /usr/local/noVNC --listen 6080 > $DOCKER_HOME/.log/novnc.log 2>&1 &

NOVNC_PID=$!
wait $NOVNC_PID