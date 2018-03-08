#!/bin/bash -x
set -eu -o pipefail

BASH_PID=$(ps ax | grep [s]tart-video-recording | awk '{ print $1'} )
FFMPEG_PID=$(ps ax | grep [f]fmpeg | awk '{ print $1'} )

kill -SIGTERM $FFMPEG_PID $BASH_PID