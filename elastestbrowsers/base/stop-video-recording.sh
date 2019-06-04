#!/bin/bash -x
set -eu -o pipefail

echo 'q' > /tmp/stop
rm /tmp/stop

# Wait for ffmpeg to finish
while [[ $(pgrep ffmpeg) ]]
do
 sleep 1 
done
