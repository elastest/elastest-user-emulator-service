#!/usr/bin/env bash

#/ Usage:
#/   EB_VERSION=2.1.0 ./build_containers.sh

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Settings
# ========

# Load old releases versions
source browsers_oldreleases

# Export MODE="FULL" to generate all containers versions
MODE="${MODE:-NIGHTLY}"

# Working directory
WORKDIR="$PWD/workdir"
[[ -d "$WORKDIR" ]] || mkdir -p "$WORKDIR"
rm -f "$WORKDIR"/*

# Download file with WebDriver versions
WEBDRIVER_VERSIONS_URL="https://raw.githubusercontent.com/bonigarcia/webdrivermanager/master/src/main/resources/versions.properties"
WEBDRIVER_VERSIONS_FILE="$WORKDIR/webdriver_versions.properties"
wget -O "$WEBDRIVER_VERSIONS_FILE" "$WEBDRIVER_VERSIONS_URL"

# Generate "versions.txt" file with browser versions, and download Selenoid
docker run --rm -t -v "$WORKDIR:/workdir" elastestbrowsers/utils-get_browsers_version:4
# shellcheck source=/dev/null
source "$WORKDIR/versions.txt"

# Build metadata
BUILD_DATE="$(date +'%Y%m%d')"
GIT_COMMIT="$(git log -1 --pretty=%h)"
GIT_URL="https://github.com/elastest/elastest-user-emulator-service"



# Util functions
# ==============

# Get Geckodriver
function get_geckodriver {
  local PARAM_VERSION="${1:-0.0.0}"

  GECKO_DRIVER_VERSION="$(grep -F "firefox${PARAM_VERSION}=" "$WEBDRIVER_VERSIONS_FILE" | cut -d"=" -f2)" || true
  if [[ -z "$GECKO_DRIVER_VERSION" ]]; then
    GECKO_DRIVER_VERSION="$(grep -E 'firefox[0-9]{2,3}=' "$WEBDRIVER_VERSIONS_FILE" | sort -r | head -n1 | cut -d"=" -f2)"
  fi

  wget -O "$WORKDIR/geckodriver.tar.gz" "https://github.com/mozilla/geckodriver/releases/download/v${GECKO_DRIVER_VERSION}/geckodriver-v${GECKO_DRIVER_VERSION}-linux64.tar.gz"
  tar xf "$WORKDIR/geckodriver.tar.gz" -C "$WORKDIR"
  rm "$WORKDIR/geckodriver.tar.gz"
  cp -p "$WORKDIR/geckodriver" image/selenoid/geckodriver
  chmod 755 image/selenoid/geckodriver
}

# Get Chromedriver
function get_chromedriver {
  local PARAM_VERSION="${1:-0.0.0}"

  CHROME_DRIVER_VERSION="$(grep -F "chrome${PARAM_VERSION}=" "$WEBDRIVER_VERSIONS_FILE" | cut -d"=" -f2)" || true
  if [[ -z "$CHROME_DRIVER_VERSION" ]]; then
    CHROME_DRIVER_VERSION="$(grep -E 'chrome[0-9]{2,3}=' "$WEBDRIVER_VERSIONS_FILE" | sort -r | head -n1 | cut -d"=" -f2)"
  fi

  wget -O "$WORKDIR/chromedriver.zip" "https://chromedriver.storage.googleapis.com/${CHROME_DRIVER_VERSION}/chromedriver_linux64.zip"
  unzip -o "$WORKDIR/chromedriver.zip" -d "$WORKDIR"
  rm "$WORKDIR/chromedriver.zip"
  cp -p "$WORKDIR/chromedriver" image/selenoid/chromedriver
  chmod 755 image/selenoid/chromedriver
}



# Build base image
# ================

pushd base/
docker build \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --tag "elastestbrowsers/utils-x11-base:$EB_VERSION" \
  .
popd # pushd base/



# Build Firefox images
# ====================

cp -p workdir/selenoid_linux_amd64 firefox/image/selenoid/selenoid_linux_amd64
pushd firefox/



# Firefox release
# ---------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox >Dockerfile
sed "s/VERSION/$FIREFOX_VER/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json

get_geckodriver "$FIREFOX_VER"

docker build \
  --build-arg VERSION="$FIREFOX_PKG" \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg GD_VERSION="$GECKO_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/firefox:${FIREFOX_VER}-${EB_VERSION}" \
  --tag "elastestbrowsers/firefox:latest-${EB_VERSION}" \
  --tag "elastestbrowsers/firefox:latest" \
  --tag "elastestbrowsers/firefox:${FIREFOX_VER}" \
  .

rm image/selenoid/browsers.json
rm image/selenoid/geckodriver



# Firefox beta
# ------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.beta >Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.beta

get_geckodriver "$FIREFOX_BETA_VER"

docker build \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg GD_VERSION="$GECKO_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/firefox:beta-${EB_VERSION}" \
  --tag "elastestbrowsers/firefox:beta" \
  .

rm image/selenoid/browsers.json.beta
rm image/selenoid/geckodriver



# Firefox nightly
# ---------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.nightly >Dockerfile
sed "s/VERSION/nightly/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.nightly

if [[ -z "$FIREFOX_NIGHTLY_VER" ]]; then
  get_geckodriver "$FIREFOX_BETA_VER"
else
  get_geckodriver "$FIREFOX_NIGHTLY_VER"
fi

docker build \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg GD_VERSION="$GECKO_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/firefox:nightly-${EB_VERSION}" \
  --tag "elastestbrowsers/firefox:nightly" \
  .

rm image/selenoid/browsers.json.nightly
rm image/selenoid/geckodriver



# Firefox old versions
# --------------------

if [[ "$MODE" == "FULL" ]]; then
  sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.older_releases >Dockerfile

  for V in $FIREFOX_OLD_VERSIONS; do
    sed "s/VERSION/$V/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json

    docker build \
      --build-arg VERSION="$V" \
      --tag "elastestbrowsers/firefox:${V}-${EB_VERSION}" \
      .

    rm image/selenoid/browsers.json
  done
fi

popd # pushd firefox/
rm firefox/image/selenoid/selenoid_linux_amd64



# Build Chrome images
# ===================

cp -p workdir/selenoid_linux_amd64 chrome/image/selenoid/selenoid_linux_amd64
pushd chrome/



# Chrome release
# --------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome >Dockerfile
sed "s/VERSION/$CHROME_VER/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json

get_chromedriver "$CHROME_VER"

docker build \
  --build-arg VERSION="$CHROME_PKG" \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg CD_VERSION="$CHROME_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/chrome:${CHROME_VER}-${EB_VERSION}" \
  --tag "elastestbrowsers/chrome:latest-${EB_VERSION}" \
  --tag "elastestbrowsers/chrome:latest" \
  --tag "elastestbrowsers/chrome:${CHROME_VER}" \
  .

rm image/selenoid/browsers.json
rm image/selenoid/chromedriver
rm "$WORKDIR/chromedriver"



# Chrome beta
# -----------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.beta >Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.beta

get_chromedriver "$CHROME_BETA_VER"

docker build \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg CD_VERSION="$CHROME_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/chrome:beta-${EB_VERSION}" \
  --tag "elastestbrowsers/chrome:beta" \
  .

rm image/selenoid/browsers.json.beta
rm image/selenoid/chromedriver
rm "$WORKDIR/chromedriver"



# Chrome unstable
# ---------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.unstable >Dockerfile
sed "s/VERSION/unstable/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.unstable

if [[ -z "${CHROME_NIGHTLY_VER:-}" ]]; then
  CHROME_NIGHTLY_VER="$((CHROME_BETA_VER + 1))"
fi
get_chromedriver "$CHROME_NIGHTLY_VER"

docker build \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg CD_VERSION="$CHROME_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/chrome:unstable-${EB_VERSION}" \
  --tag "elastestbrowsers/chrome:unstable" \
  .

rm image/selenoid/browsers.json.unstable
rm image/selenoid/chromedriver
rm "$WORKDIR/chromedriver"



# Chrome old versions
# -------------------

if [[ "$MODE" == "FULL" ]]; then
  sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.older_releases >Dockerfile

  for V in $CHROME_OLD_VERSIONS; do
    TAG_VER="$(echo "$V" | cut -d"." -f1,2)"
    TAG_VER_MAJOR="$(echo "$V" | cut -d"." -f1)"
    if [[ "$TAG_VER_MAJOR" -lt 72 ]]; then
      sed "s/VERSION/$V/g" image/selenoid/browsers.json-oldversions.templ >image/selenoid/browsers.json
    else
      sed "s/VERSION/$V/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json
    fi

    case "$TAG_VER" in
      "60.0") CHROMEDRIVER="2.33" ;;
      "61.0") CHROMEDRIVER="2.34" ;;
      "62.0") CHROMEDRIVER="2.34" ;;
      "63.0") CHROMEDRIVER="2.36" ;;
      *) CHROMEDRIVER="2.37" ;;
    esac

    wget -O "$WORKDIR/chromedriver.zip" "https://chromedriver.storage.googleapis.com/$CHROMEDRIVER/chromedriver_linux64.zip"
    unzip "$WORKDIR/chromedriver.zip" -d image/selenoid
    rm chromedriver.zip

    docker build \
      --build-arg VERSION="$V" \
      --tag "elastestbrowsers/chrome:${TAG_VER}-${EB_VERSION}" \
      .

    rm image/selenoid/chromedriver
    rm image/selenoid/browsers.json
  done
fi

popd # pushd chrome/
rm chrome/image/selenoid/selenoid_linux_amd64



# Show results
# ============

echo "Done! Current Docker images:"
docker image ls
