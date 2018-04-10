#!/bin/bash -x
set -e

### Variables ###
[ -d "$DOCKER_HOME/recordings" ] || mkdir $DOCKER_HOME/recordings
RESOLUTION="${RESOLUTION:-1440x900}"
DISPLAY=:0.0
VIDEO_FORMAT="${VIDEO_FORMAT:-mp4}"

if [ ! "$#" -eq 2 ]; then
  echo "Usage: $0 -n VIDEO_NAME"
  exit 1
fi

while getopts "n:" opt; do
  case "${opt}" in
    n)
      VIDEO_NAME=${OPTARG}
      ;;
    *)
      echo "Usage: $0 -n VIDEO_NAME"
      exit 1
      ;;
  esac
done

### Only one recording at a time
FFMPEG_PID=$(ps ax | grep [f]fmpeg | awk '{ print $1'} )
if [ ! -z "$FFMPEG_PID" ]; then
	echo "There is a recording in progress..."
	exit 1
fi

touch /tmp/stop

# Force to be able to write the file on disk
sudo chmod 777 $DOCKER_HOME/recordings

### Start recording with ffmpeg ###
</tmp/stop ffmpeg -y -f alsa -i pulse -f x11grab -framerate 25 -video_size $RESOLUTION -i $DISPLAY -c:a libfdk_aac -c:v libx264 -preset ultrafast -crf 28 -refs 4 -qmin 4 -pix_fmt yuv420p -filter:v fps=25 ~/recordings/${VIDEO_NAME}.${VIDEO_FORMAT}
