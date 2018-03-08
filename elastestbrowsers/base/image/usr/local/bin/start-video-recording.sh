#!/bin/bash -x
set -e

### Variables ###
[ -d "$DOCKER_HOME/recordings" ] || mkdir $DOCKER_HOME/recordings
RESOLUTION="${RESOLUTION:-1440x900}"
VIDEO_NAME=video-$(date +%s)
DISPLAY=:0.0

### Only one recording at a time
FFMPEG_PID=$(ps ax | grep [f]fmpeg | awk '{ print $1'} )
if [ ! -z "$FFMPEG_PID" ]; then
	echo "There is a recording in progress..."
	exit 1
fi

touch stop

### Start recording with ffmpeg ###

<./stop ffmpeg -y -f alsa -i pulse -f x11grab -framerate 25 -video_size $RESOLUTION -i $DISPLAY -c:a libfdk_aac -c:v libx264 -preset ultrafast -crf 28 -refs 4 -qmin 4 -pix_fmt yuv420p -filter:v fps=25 ~/recordings/${VIDEO_NAME}.mkv

