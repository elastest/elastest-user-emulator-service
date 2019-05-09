#!/bin/bash -x
set -eu -o pipefail

# Load old releases versions
. browsers_oldreleases

# File with webdrivers versions
WEBDRIVER_VERSIONS="https://raw.githubusercontent.com/bonigarcia/webdrivermanager/master/src/main/resources/versions.properties"

# Use MODE=FULL to generate all containers versions
MODE=${MODE:-NIGHTLY}

WORKDIR=$PWD/workdir
[ -d $WORKDIR ] || mkdir -p $WORKDIR
rm $WORKDIR/* || true

# Get Geckodriver
get_geckodriver () {

  set +o pipefail
  GECKO_VERSION=$(curl --silent ${WEBDRIVER_VERSIONS} | grep "firefox${1}" | cut -d"=" -f2)
  set -o pipefail
  if [ -z "${GECKO_VERSION}"]; then
    GECKO_VERSION=$(curl --silent ${WEBDRIVER_VERSIONS} | grep -E 'firefox[0-9]{2,3}=' | head -n1 | cut -d"=" -f2)
  fi
  wget -O $WORKDIR/geckodriver.tar.gz https://github.com/mozilla/geckodriver/releases/download/v$GECKO_VERSION/geckodriver-v$GECKO_VERSION-linux64.tar.gz
  tar xvzf $WORKDIR/geckodriver.tar.gz -C $WORKDIR
  rm -Rfv $WORKDIR/geckodriver.tar.gz
  cp -p $WORKDIR/geckodriver image/selenoid/geckodriver
}

# Get Chromedriver
get_chromedriver () {

  set +o pipefail
  CHROME_DRIVER_VER=$(curl --silent ${WEBDRIVER_VERSIONS} | grep "chrome${1}" | cut -d"=" -f2)
  if [ -z "${CHROME_DRIVER_VER}" ]; then
    CHROME_DRIVER_VER=$(curl --silent ${WEBDRIVER_VERSIONS} | grep -E "chrome[0-9]{2,3}=" | head -n1 | cut -d"=" -f2)
  fi
  set -o pipefail
  wget -O $WORKDIR/chromedriver.zip https://chromedriver.storage.googleapis.com/$CHROME_DRIVER_VER/chromedriver_linux64.zip
  yes | unzip $WORKDIR/chromedriver.zip -d $WORKDIR
  rm -Rfv $WORKDIR/chromedriver.zip
  cp -p $WORKDIR/chromedriver image/selenoid/chromedriver
}

# Get browsers versions
docker run -t --rm -v $WORKDIR:/workdir elastestbrowsers/utils-get_browsers_version:3
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

# Copy Selenoid driver
cp -p workdir/selenoid_linux_amd64 firefox/image/selenoid/selenoid_linux_amd64

###########
# Firefox
###########
pushd firefox
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox > Dockerfile
sed "s/VERSION/$FIREFOX_VER/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json

get_geckodriver ${FIREFOX_VER}

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
  --tag elastestbrowsers/firefox:$FIREFOX_VER \
  --file Dockerfile .

# Cleaning
rm image/selenoid/browsers.json
rm image/selenoid/geckodriver

# Firefox Beta
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.beta > Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.beta

get_geckodriver ${FIREFOX_BETA_VER}

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
rm image/selenoid/geckodriver

# Firefox Nightly
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.nightly > Dockerfile
sed "s/VERSION/nightly/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.nightly

if [ -z "${FIREFOX_NIGHTLY_VER}" ]; then
  get_geckodriver ${FIREFOX_BETA_VER}
else
  get_geckodriver ${FIREFOX_NIGHTLY_VER}
fi

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
rm image/selenoid/geckodriver

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
rm firefox/image/selenoid/selenoid_linux_amd64

# Copy Selenoid driver
cp -p workdir/selenoid_linux_amd64 chrome/image/selenoid/selenoid_linux_amd64

###########
# Chome
###########
pushd chrome
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome > Dockerfile 
sed "s/VERSION/$CHROME_VER/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json

get_chromedriver ${CHROME_VER}

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
  --tag elastestbrowsers/chrome:${CHROME_VER} \
  --file Dockerfile .

rm image/selenoid/browsers.json
rm image/selenoid/chromedriver
rm $WORKDIR/chromedriver

# Chrome Beta
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.beta > Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.beta

get_chromedriver ${CHROME_BETA_VER}

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
rm image/selenoid/chromedriver
rm $WORKDIR/chromedriver

# Chrome Unstable
sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.unstable > Dockerfile
sed "s/VERSION/unstable/g" image/selenoid/browsers.json.templ > image/selenoid/browsers.json.unstable

get_chromedriver ${CHROME_NIGHTLY_VER}

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
rm image/selenoid/chromedriver
rm $WORKDIR/chromedriver

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
rm chrome/image/selenoid/selenoid_linux_amd64

docker images

