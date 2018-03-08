#!/bin/bash -x
set -e

WORKDIR=$PWD/workdir
. $WORKDIR/versions.txt

docker push elastestbrowsers/utils-x11-base:1.1
  
docker push elastestbrowsers/chrome:latest
docker push elastestbrowsers/chrome:$CHROME_VER
docker push elastestbrowsers/chrome:beta
docker push elastestbrowsers/chrome:unstable

docker push elastestbrowsers/firefox:latest
docker push elastestbrowsers/firefox:$FIREFOX_VER
docker push elastestbrowsers/firefox:beta
docker push elastestbrowsers/firefox:nightly
  
docker logout
