#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

#/ Usage:
#/
#/ EB_VERSION="2.2.0" MODE="NIGHTLY" ./build_containers.sh
#/
#/ Environment:
#/
#/ * `EB_VERSION`
#/   The version number that should be used to tag *ElastestBrowsers* Docker images.
#/   Required.
#/
#/ * `MODE`
#/   Whether to build all possible browser versions, or just the latest available ones:
#/   - `MODE="NIGHTLY"`: build only the latest browser versions that are currently available in official repositories. This is the default.
#/   - `MODE="FULL"`: build both the latest *and* all the older browser versions (which are defined with `FIREFOX_VERSIONS`).
#/   Optional. Default: "NIGHTLY".
#/

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Settings
# ========

# Load old releases versions
# shellcheck source=browser_old_versions.conf.sh
source browser_old_versions.conf.sh

# Provide MODE="FULL" to enable generation of all browser versions
MODE="${MODE:-NIGHTLY}"

# Working directory
WORKDIR="$PWD/workdir"
mkdir -p "$WORKDIR"
rm -f "$WORKDIR"/*

# Download file with WebDriver versions
WEBDRIVER_VERSIONS_URL="https://raw.githubusercontent.com/bonigarcia/webdrivermanager/master/src/main/resources/versions.properties"
WEBDRIVER_VERSIONS_FILE="$WORKDIR/webdriver_versions.properties"
wget --no-verbose -O "$WEBDRIVER_VERSIONS_FILE" "$WEBDRIVER_VERSIONS_URL"

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
# Argument: Major version
function get_geckodriver {
  local ARG_VERSION_MAJ="${1:-0}"

  GECKO_DRIVER_VERSION="$(grep -F "firefox${ARG_VERSION_MAJ}=" "$WEBDRIVER_VERSIONS_FILE" | cut -d"=" -f2)" || true
  if [[ -z "${GECKO_DRIVER_VERSION:-}" ]]; then
    # Assume we need the latest available driver version
    GECKO_DRIVER_VERSION="$(grep -E 'firefox[0-9]+=' "$WEBDRIVER_VERSIONS_FILE" | sort -r | head -n1 | cut -d"=" -f2)"
  fi

  wget --no-verbose -O "$WORKDIR/geckodriver.tar.gz" "https://github.com/mozilla/geckodriver/releases/download/v${GECKO_DRIVER_VERSION}/geckodriver-v${GECKO_DRIVER_VERSION}-linux64.tar.gz"
  tar xf "$WORKDIR/geckodriver.tar.gz" -C "$WORKDIR"
  rm "$WORKDIR/geckodriver.tar.gz"
  cp -p "$WORKDIR/geckodriver" image/selenoid/geckodriver
  chmod 755 image/selenoid/geckodriver
}

# Get Chromedriver
# Argument: Major version
function get_chromedriver {
  local ARG_VERSION_MAJ="${1:-0}"

  CHROME_DRIVER_VERSION="$(grep -F "chrome${ARG_VERSION_MAJ}=" "$WEBDRIVER_VERSIONS_FILE" | cut -d"=" -f2)" || true
  if [[ -z "${CHROME_DRIVER_VERSION:-}" ]]; then
    # Assume we need the latest available driver version
    CHROME_DRIVER_VERSION="$(grep -E 'chrome[0-9]+=' "$WEBDRIVER_VERSIONS_FILE" | sort -r | head -n1 | cut -d"=" -f2)"
  fi

  wget --no-verbose -O "$WORKDIR/chromedriver.zip" "https://chromedriver.storage.googleapis.com/${CHROME_DRIVER_VERSION}/chromedriver_linux64.zip"
  unzip -o "$WORKDIR/chromedriver.zip" -d "$WORKDIR"
  rm "$WORKDIR/chromedriver.zip"
  mv "$WORKDIR/chromedriver" image/selenoid/chromedriver
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
  --tag "elastestbrowsers/utils-x11-base:${EB_VERSION}" \
  .
popd # base/



# Build Firefox images
# ====================

cp -p "$WORKDIR/selenoid_linux_amd64" firefox/image/selenoid/selenoid_linux_amd64
pushd firefox/



# Firefox release
# ---------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox >Dockerfile
sed "s/VERSION/$FIREFOX_VERSION_MAJ/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json
get_geckodriver "$FIREFOX_VERSION_MAJ"

docker build \
  --build-arg VERSION="$FIREFOX_VERSION" \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg GD_VERSION="$GECKO_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/firefox:${FIREFOX_VERSION_MAJ}-${EB_VERSION}" \
  --tag "elastestbrowsers/firefox:${FIREFOX_VERSION_MAJ}" \
  --tag "elastestbrowsers/firefox:latest-${EB_VERSION}" \
  --tag "elastestbrowsers/firefox:latest" \
  .

rm Dockerfile
rm image/selenoid/browsers.json
rm image/selenoid/geckodriver



# Firefox beta
# ------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.beta >Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.beta
get_geckodriver "$FIREFOX_BETA_VERSION_MAJ"

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

rm Dockerfile
rm image/selenoid/browsers.json.beta
rm image/selenoid/geckodriver



# Firefox nightly
# ---------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.nightly >Dockerfile
sed "s/VERSION/nightly/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.nightly
get_geckodriver "$FIREFOX_NIGHTLY_VERSION_MAJ"

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

rm Dockerfile
rm image/selenoid/browsers.json.nightly
rm image/selenoid/geckodriver



# Firefox old versions
# --------------------

if [[ "$MODE" == "FULL" ]]; then
  sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.firefox.old_versions >Dockerfile

  for FIREFOX_VERSION in "${FIREFOX_VERSIONS[@]}"; do
    FIREFOX_VERSION_MAJ="${FIREFOX_VERSION%%.*}"

    sed "s/VERSION/$FIREFOX_VERSION/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json

    get_geckodriver "$FIREFOX_VERSION_MAJ"

    docker build \
      --build-arg DOWNLOAD_URL="https://ftp.mozilla.org/pub/firefox/releases/${FIREFOX_VERSION}/linux-x86_64/en-US/firefox-${FIREFOX_VERSION}.tar.bz2" \
      --tag "elastestbrowsers/firefox:${FIREFOX_VERSION_MAJ}-${EB_VERSION}" \
      .

    rm image/selenoid/browsers.json
    rm image/selenoid/geckodriver
  done

  rm Dockerfile
fi

popd # firefox/
rm firefox/image/selenoid/selenoid_linux_amd64



# Build Chrome images
# ===================

cp -p "$WORKDIR/selenoid_linux_amd64" chrome/image/selenoid/selenoid_linux_amd64
pushd chrome/



# Chrome release
# --------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome >Dockerfile
sed "s/VERSION/$CHROME_VERSION_MAJ/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json
get_chromedriver "$CHROME_VERSION_MAJ"

docker build \
  --build-arg VERSION="$CHROME_VERSION" \
  --build-arg EB_VERSION="$EB_VERSION" \
  --build-arg BUILD_DATE="$BUILD_DATE" \
  --build-arg GIT_COMMIT="$GIT_COMMIT" \
  --build-arg GIT_URL="$GIT_URL" \
  --build-arg CD_VERSION="$CHROME_DRIVER_VERSION" \
  --build-arg SELENOID_VERSION="$SELENOID_VERSION" \
  --tag "elastestbrowsers/chrome:${CHROME_VERSION_MAJ}-${EB_VERSION}" \
  --tag "elastestbrowsers/chrome:${CHROME_VERSION_MAJ}" \
  --tag "elastestbrowsers/chrome:latest-${EB_VERSION}" \
  --tag "elastestbrowsers/chrome:latest" \
  .

rm Dockerfile
rm image/selenoid/browsers.json
rm image/selenoid/chromedriver



# Chrome beta
# -----------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.beta >Dockerfile
sed "s/VERSION/beta/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.beta
get_chromedriver "$CHROME_BETA_VERSION_MAJ"

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

rm Dockerfile
rm image/selenoid/browsers.json.beta
rm image/selenoid/chromedriver



# Chrome unstable
# ---------------

sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.unstable >Dockerfile
sed "s/VERSION/unstable/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json.unstable
get_chromedriver "$CHROME_UNSTABLE_VERSION_MAJ"

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

rm Dockerfile
rm image/selenoid/browsers.json.unstable
rm image/selenoid/chromedriver



# Chrome old versions
# -------------------

if [[ "$MODE" == "FULL" ]]; then
  sed "s/@@EB_VERSION@@/$EB_VERSION/" Dockerfile.chrome.old_versions >Dockerfile

  for CHROME_VERSION in "${CHROME_VERSIONS[@]}"; do
    CHROME_VERSION_MAJ="${CHROME_VERSION%%.*}"

    if [[ "$CHROME_VERSION_MAJ" -lt 72 ]]; then
      sed "s/VERSION/$CHROME_VERSION/g" image/selenoid/browsers.json.templ.old_versions >image/selenoid/browsers.json
    else
      sed "s/VERSION/$CHROME_VERSION/g" image/selenoid/browsers.json.templ >image/selenoid/browsers.json
    fi

    get_chromedriver "$CHROME_VERSION_MAJ"

    if [[ "$CHROME_VERSION_MAJ" -lt 69 ]]; then
      DOWNLOAD_URL="https://www.slimjet.com/chrome/download-chrome.php?file=lnx%2Fchrome64_${CHROME_VERSION}.deb"
    else
      DOWNLOAD_URL="https://www.slimjet.com/chrome/download-chrome.php?file=files%2F${CHROME_VERSION}%2Fgoogle-chrome-stable_current_amd64.deb"
    fi

    docker build \
      --build-arg DOWNLOAD_URL="$DOWNLOAD_URL" \
      --tag "elastestbrowsers/chrome:${CHROME_VERSION_MAJ}-${EB_VERSION}" \
      .

    rm image/selenoid/browsers.json
    rm image/selenoid/chromedriver
  done

  rm Dockerfile
fi

popd # chrome/
rm chrome/image/selenoid/selenoid_linux_amd64



# Show results
# ============

echo "Done! Current Docker images:"
docker image ls
