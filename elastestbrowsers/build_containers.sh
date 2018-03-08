#!/bin/bash -x
set -e

UPLOAD=${PUSH:=false}

WORKDIR=$PWD/workdir
[ -d $WORKDIR ] || mkdir -p $WORKDIR
rm $WORKDIR/* || true

# Get browsers versions
docker run -d --rm -v $WORKDIR:/workdir elastestbrowsers/utils-get_browsers_version:1.0
. $WORKDIR/versions.txt

# Build base image
pushd base
docker build -t elastestbrowsers/utils-x11-base:1.1 .
popd

# Copy drivers
cp -p workdir/geckodriver firefox/image/selenoid/geckodriver
cp -p workdir/selenoid_linux_amd64 firefox/image/selenoid/selenoid_linux_amd64

# Firefox
pushd firefox
sed "s/VERSION/$FIREFOX_VER/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json
docker build --build-arg VERSION=$FIREFOX_PKG -t elastestbrowsers/firefox:$FIREFOX_VER -t elastestbrowsers/firefox:latest -f Dockerfile.firefox .
rm image/selenoid/browsers.json

# Firefox Beta
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.beta
docker build -t elastestbrowsers/firefox:beta -f Dockerfile.firefox.beta .
rm image/selenoid/browsers.json.beta

# Firefox Nightly
sed "s/VERSION/nightly/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.nightly
docker build -t elastestbrowsers/firefox:nightly -f Dockerfile.firefox.nightly .
rm image/selenoid/browsers.json.nightly
popd

# cleaning
rm firefox/image/selenoid/geckodriver
rm firefox/image/selenoid/selenoid_linux_amd64

# Copy drivers
cp -p workdir/chromedriver chrome/image/selenoid/chromedriver
cp -p workdir/selenoid_linux_amd64 chrome/image/selenoid/selenoid_linux_amd64

# Chome
pushd chrome
sed "s/VERSION/$CHROME_VER/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json
docker build --build-arg VERSION=$CHROME_PKG -t elastestbrowsers/chrome:$CHROME_VER -t elastestbrowsers/chrome:latest -f Dockerfile.chrome .
rm image/selenoid/browsers.json

# Chrome Beta
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.beta
docker build -t elastestbrowsers/chrome:beta -f Dockerfile.chrome.beta .
rm image/selenoid/browsers.json.beta

# Chrome Unstable
sed "s/VERSION/unstable/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.unstable
docker build -t elastestbrowsers/chrome:unstable -f Dockerfile.chrome.unstable .
rm image/selenoid/browsers.json.unstable
popd

# cleaning
rm chrome/image/selenoid/chromedriver
rm chrome/image/selenoid/selenoid_linux_amd64

rm -rf $WORKDIR
docker images

if [ "$UPLOAD" == true ]; then
  docker login
  
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
fi
