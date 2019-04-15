#!/bin/bash -x
set -eu -o pipefail

# Load old releases versions
. browsers_oldreleases

# Use MODE=FULL to generate all containers versions
MODE=${MODE:-NIGHTLY}

WORKDIR=$PWD/workdir
[ -d $WORKDIR ] || mkdir -p $WORKDIR
rm $WORKDIR/* || true

# Get browsers versions
docker run -t --rm -v $WORKDIR:/workdir elastestbrowsers/utils-get_browsers_version:2
. $WORKDIR/versions.txt

# Build base image
GIT_COMMIT=$(git log -1 --pretty=%h)
BUILD_DATE=$(date +'%Y%m%d')
GIT_URL="https://github.com/elastest/elastest-user-emulator-service"

pushd base
docker build --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --tag elastestbrowsers/utils-x11-base:${EB_VERSION} .
popd

# Copy drivers
cp -p workdir/geckodriver firefox/image/selenoid/geckodriver
cp -p workdir/selenoid_linux_amd64 firefox/image/selenoid/selenoid_linux_amd64

###########
# Firefox
###########
pushd firefox
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox > Dockerfile
sed "s/VERSION/$FIREFOX_VER/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json

docker build --build-arg VERSION=$FIREFOX_PKG \
  --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --build-arg GD_VERSION=${GECKO_VERSION} \
  --build-arg SELENOID_VERSION=${SELENOID_VERSION} \
  --tag elastestbrowsers/firefox:$FIREFOX_VER-${EB_VERSION} \
  --tag elastestbrowsers/firefox:latest-${EB_VERSION} \
  --tag elastestbrowsers/firefox:latest \
  --file Dockerfile .
rm image/selenoid/browsers.json

# Firefox Beta
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.beta > Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.beta

docker build --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --build-arg GD_VERSION=${GECKO_VERSION} \
  --build-arg SELENOID_VERSION=${SELENOID_VERSION} \
  --tag elastestbrowsers/firefox:beta-${EB_VERSION} \
  --tag elastestbrowsers/firefox:beta \
  --file Dockerfile .
rm image/selenoid/browsers.json.beta

# Firefox Nightly
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.nightly > Dockerfile
sed "s/VERSION/nightly/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.nightly

docker build --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --build-arg GD_VERSION=${GECKO_VERSION} \
  --build-arg SELENOID_VERSION=${SELENOID_VERSION} \
  --tag elastestbrowsers/firefox:nightly-${EB_VERSION} \
  --tag elastestbrowsers/firefox:nightly \
  --file Dockerfile .
rm image/selenoid/browsers.json.nightly

# Firefox old versions
if [ "$MODE" == "FULL" ]; then
  sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.older_releases > Dockerfile
	for V in $FIREFOX_OLD_VERSIONS
	do
		sed "s/VERSION/$V/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json
		docker build --build-arg VERSION=$V -t elastestbrowsers/firefox:$V-${EB_VERSION} -f Dockerfile .
		rm image/selenoid/browsers.json
	done
fi
popd

# cleaning
rm firefox/image/selenoid/geckodriver
rm firefox/image/selenoid/selenoid_linux_amd64

# Copy drivers
cp -p workdir/chromedriver chrome/image/selenoid/chromedriver
cp -p workdir/selenoid_linux_amd64 chrome/image/selenoid/selenoid_linux_amd64

###########
# Chome
###########
pushd chrome
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome > Dockerfile 
sed "s/VERSION/$CHROME_VER/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json

docker build --build-arg VERSION=$CHROME_PKG \
  --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --build-arg CD_VERSION=${CHROME_DRIVER_VER} \
  --build-arg SELENOID_VERSION=${SELENOID_VERSION} \
  --tag elastestbrowsers/chrome:${CHROME_VER}-${EB_VERSION} \
  --tag elastestbrowsers/chrome:latest-${EB_VERSION} \
  --tag elastestbrowsers/chrome:latest \
  --file Dockerfile .
rm image/selenoid/browsers.json

# Chrome Beta
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.beta > Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.beta

docker build --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --build-arg CD_VERSION=${CHROME_DRIVER_VER} \
  --build-arg SELENOID_VERSION=${SELENOID_VERSION} \
  --tag elastestbrowsers/chrome:beta-${EB_VERSION} \
  --tag elastestbrowsers/chrome:beta \
  --file Dockerfile .
rm image/selenoid/browsers.json.beta

# Chrome Unstable
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.unstable > Dockerfile
sed "s/VERSION/unstable/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.unstable

docker build --build-arg EB_VERSION=${EB_VERSION} \
  --build-arg GIT_URL=${GIT_URL} \
  --build-arg GIT_COMMIT=${GIT_COMMIT} \
  --build-arg BUILD_DATE=${BUILD_DATE} \
  --build-arg CD_VERSION=${CHROME_DRIVER_VER} \
  --build-arg SELENOID_VERSION=${SELENOID_VERSION} \
  --tag elastestbrowsers/chrome:unstable-${EB_VERSION} \
  --tag elastestbrowsers/chrome:unstable \
  --file Dockerfile .
rm image/selenoid/browsers.json.unstable

# Chrome old versions
if [ "$MODE" == "FULL" ]; then
  sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.older_releases > Dockerfile
	for V in $CHROME_OLD_VERSIONS
	do
		rm -Rfv image/selenoid/chromedriver
		TAG_VER=$(echo $V | cut -d"." -f1,2)
    TAG_VER_MAJOR=$(echo $V | cut -d"." -f1)
    if [ "${TAG_VER_MAJOR}" -lt "72" ]; then
      sed "s/VERSION/$V/g" image/selenoid/browsers.json-oldversions.templ > image/selenoid/browsers.json
    else
      sed "s/VERSION/$V/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json
    fi
		case $TAG_VER in
			"60.0")
				CHROMEDRIVER=2.33
				;;
			"61.0" | "62.0")
				CHROMEDRIVER=2.34
				;;
			"63.0")
				CHROMEDRIVER=2.36
				;;
			*)
				CHROMEDRIVER=2.37
				;;
		esac
		wget -O $WORKDIR/chromedriver.zip https://chromedriver.storage.googleapis.com/$CHROMEDRIVER/chromedriver_linux64.zip && \
		unzip $WORKDIR/chromedriver.zip -d image/selenoid && \
		rm -Rfv /chromedriver.zip
		docker build --build-arg VERSION=$V -t elastestbrowsers/chrome:$TAG_VER-${EB_VERSION} -f Dockerfile .
		rm image/selenoid/browsers.json
	done
fi 

popd

# cleaning
rm chrome/image/selenoid/chromedriver
rm chrome/image/selenoid/selenoid_linux_amd64

docker images

