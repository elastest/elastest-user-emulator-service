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
VIDEO_FORMAT="${VIDEO_FORMAT:-mp4}"
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

touch /tmp/stop

# Force to be able to write the file on disk
sudo chmod 777 $HOME/recordings

### Start recording with ffmpeg ###
</tmp/stop ffmpeg -y -f alsa -i pulse -f x11grab -framerate 25 -video_size $RESOLUTION -i $DISPLAY -c:a libfdk_aac -c:v libx264 -preset ultrafast -crf 28 -refs 4 -qmin 4 -pix_fmt yuv420p -filter:v fps=25 $HOME/recordings/${VIDEO_NAME}.${VIDEO_FORMAT}
