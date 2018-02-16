#!/bin/bash

while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -s|--start)
    flvrec.py -P passwd_file -o $2.flv $3 $4
    shift
    ;;
    -e|--end)
    killall flvrec.py
    shift
    ;;
    -c|--convert)
    touch $3
    ffmpeg -y -i $2.flv -pix_fmt yuv420p -c:v libx264 -crf 19 -strict experimental $3
    shift
    ;;
    -u|--upload)
    response=$(curl -X POST $2api/v1/paths//$3/create-file)
    curl -X POST $2api/v1/streams/$response/write --data-binary @$3 -H "Content-Type: application/octet-stream"
    curl -X POST $2api/v1/streams/$response/close
    shift
    ;;
    -d|--delete)
    curl -X POST $2api/v1/paths//$3/delete
    shift
    ;;
esac
shift
done

