#!/bin/bash -x
SCREEN_RESOLUTION=${SCREEN_RESOLUTION:-"1440x1080x24"}
ENABLE_WINDOW_MANAGER=${ENABLE_WINDOW_MANAGER:-""}
DISPLAY=99

# Starting selenoid
/usr/bin/xvfb-run -l -n "$DISPLAY" -s "-ac -screen 0 $SCREEN_RESOLUTION -noreset -listen tcp" /usr/local/bin/selenoid -conf /etc/browsers.json -disable-docker -timeout 1h -enable-file-upload -capture-driver-logs &

# Starting VNC
x11vnc -display ":$DISPLAY" -passwd selenoid -shared -forever -loop500 -rfbport 5900 -rfbportv6 5900 -logfile /home/ubuntu/x11vnc.log &

# Starting novnc
/usr/local/noVNC/utils/launch.sh --web /usr/local/noVNC --listen 6080 > /home/ubuntu/novnc.log &

# Starting fluxbox
fluxbox -display :$DISPLAY &

# Starting pulseaudio 
pulseaudio -D

wait