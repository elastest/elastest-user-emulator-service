#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



REC_DIR="$HOME/recordings"
if [[ ! -d "$REC_DIR" ]]; then
  mkdir -p "$REC_DIR"
  chmod 777 "$REC_DIR" # Ensure write permissions
fi

RESOLUTION="${RESOLUTION:-1440x1080}"
DISPLAY=":99"

if [[ "$#" -ne 2 ]]; then
  echo "Usage: $0 -n VIDEO_NAME"
  exit 1
fi

while getopts "n:" opt; do
  case "${opt}" in
    n)
      VIDEO_NAME="${OPTARG}"
      ;;
    *)
      echo "Usage: $0 -n VIDEO_NAME"
      exit 1
      ;;
  esac
done

# Allow only one recording at the same time
if pgrep --exact ffmpeg >/dev/null; then
  echo "Abort: there is already a recording in progress"
  exit 1
fi

REC_PATH="${REC_DIR}/${VIDEO_NAME}.mp4"

# Start recording with FFmpeg
#
# NOTES:
#
#   -f x11grab -framerate 25
#     This configures the input device (x11grab) to generate an input stream by
#     capturing the screen 25 times per second.
#
#   -r 25
#     This tells FFmpeg 2 things:
#     1. That it should assume an input rate of 25 frames per second.
#     2. That the input timestamps should be ignored, and instead new timestamps
#        should be generated from scratch.
#
#   Other options:
#     https://ffmpeg.org/ffmpeg-all.html#Video-Options
#

# Record audio+video
ffmpeg \
  -nostdin \
  -f alsa -ar 44100 -ac 2 \
  -thread_queue_size 512 \
  -i pulse \
  -f x11grab -framerate 25 -r 25 -s "$RESOLUTION" \
  -i "$DISPLAY" \
  -codec:a aac \
  -codec:v libx264 -preset ultrafast -tune stillimage -pix_fmt yuv420p \
  -f mp4 -strict experimental \
  -y "$REC_PATH" \
  2>&1

# Record video only
# ffmpeg \
#   -nostdin \
#   -f x11grab -framerate 25 -r 25 -s "$RESOLUTION" \
#   -i "$DISPLAY" \
#   -codec:v libx264 -preset ultrafast -tune stillimage -pix_fmt yuv420p \
#   -f mp4 -strict experimental \
#   -y "$REC_PATH" \
#   2>&1
