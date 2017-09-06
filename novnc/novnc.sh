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
    ffmpeg -i sessionId $2.flv -c:v libx264 -crf 19 -strict experimental
    shift
    ;;
    -c|--convert)
    ffmpeg -i $2.flv -c:v libx264 -crf 19 -strict experimental $3
    shift
    ;;
    -u|--upload)
    curl -v -X POST $2paths//$3/create-file

    curl -v -X POST $2streams/1/write --data-binary @LICENSE -H "Content-Type: application/octet-stream"
    curl -v -X POST $2streams/1/close
    shift
    ;;
    -r|--read)
    curl -v -X POST $2paths//$3/open-file
    curl -v -X POST $2streams/1/write --data-binary @LICENSE -H "Content-Type: application/octet-stream"
    curl -v -X POST $2streams/1/close
    shift
    ;;
esac
shift
done

