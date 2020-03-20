#!/bin/bash
# File checked with ShellCheck (https://www.shellcheck.net/)

# First of all: create a lock file that tells the host we're not ready yet
touch /home/ubuntu/entrypoint.lock



# Bash configuration
# ==================

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Error trap (runs whenever an error happens)
on_error() { RC=$?; }
trap on_error ERR

# Exit trap (runs whenever the script exits, including errors due to 'errexit')
on_exit() {
    RC=${RC-$?}
    echo -n "[entrypoint] "
    if ((RC)); then echo "ERROR (code: $RC)"; else echo "SUCCESS"; fi
}
trap on_exit EXIT



# Settings
# ========

export SCREEN_RESOLUTION="${SCREEN_RESOLUTION:-1440x1080x24}"
export DISPLAY=":99"



# Start PulseAudio
# ================

echo "Start pulseaudio"

pulseaudio --daemonize \
  --log-target=file:/home/ubuntu/pulseaudio.log \
  --log-level=4 \
  &

while ! pgrep --exact pulseaudio >/dev/null; do
  echo "Wait for pulseaudio -- 500 ms..."
  sleep 0.500s
done



# Start Selenoid
# ==============

echo "Start xvfb-run selenoid"

DISPLAY_NUM="${DISPLAY#:}"
mkdir -p /home/ubuntu/selenoid-logs/

/usr/bin/xvfb-run \
  --listen-tcp \
  --server-num="$DISPLAY_NUM" \
  --server-args="-ac -listen tcp -noreset -screen 0 $SCREEN_RESOLUTION" \
  --error-file=/home/ubuntu/xvfb.log \
  /usr/local/bin/selenoid \
    -conf /etc/browsers.json \
    -disable-docker \
    -timeout 1h \
    -enable-file-upload \
    -save-all-logs \
    -log-output-dir /home/ubuntu/selenoid-logs \
    >/home/ubuntu/selenoid-logs/selenoid.log \
    &

while ! pgrep --exact Xvfb >/dev/null; do
  echo "Wait for Xvfb -- 500 ms..."
  sleep 0.500s
done



# Start VNC
# =========

echo "Start x11vnc"

x11vnc \
  -display "$DISPLAY" \
  -passwd selenoid \
  -shared -forever \
  -loop500 \
  -rfbport 5900 \
  -rfbportv6 5900 \
  >/home/ubuntu/x11vnc.log 2>&1 \
  &

while ! pgrep --exact x11vnc >/dev/null; do
  echo "Wait for x11vnc -- 500 ms..."
  sleep 0.500s
done

# Start novnc
echo "Start noVNC/utils/launch.sh"

/usr/local/noVNC/utils/launch.sh \
  --web /usr/local/noVNC \
  --listen 6080 \
  >/home/ubuntu/novnc.log 2>&1 \
  &



# Start Fluxbox
# =============
echo "Start fluxbox"
fluxbox -display "$DISPLAY" >/dev/null 2>&1 &

while ! pgrep --exact fluxbox >/dev/null; do
  echo "Wait for fluxbox -- 500 ms..."
  sleep 0.500s
done



# Remove the lock file to tell the host we're now ready to rock
rm -f /home/ubuntu/entrypoint.lock

# Block here (wait for all background shell jobs to finish)
wait
