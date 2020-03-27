#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Load old releases versions
# shellcheck source=browser_old_versions.conf.sh
source browser_old_versions.conf.sh

# Provide MODE="FULL" to enable generation of all browser versions
MODE="${MODE:-NIGHTLY}"

# Source "versions.txt" file with browser versions, and download Selenoid
WORKDIR="$PWD/workdir"
# shellcheck source=/dev/null
source "$WORKDIR/versions.txt"



docker push "elastestbrowsers/utils-x11-base:${EB_VERSION}"

docker push "elastestbrowsers/chrome:${CHROME_VERSION_MAJ}-${EB_VERSION}"
docker push "elastestbrowsers/chrome:${CHROME_VERSION_MAJ}"
docker push "elastestbrowsers/chrome:latest-${EB_VERSION}"
docker push "elastestbrowsers/chrome:latest"
docker push "elastestbrowsers/chrome:beta-${EB_VERSION}"
docker push "elastestbrowsers/chrome:beta"
docker push "elastestbrowsers/chrome:unstable-${EB_VERSION}"
docker push "elastestbrowsers/chrome:unstable"

docker push "elastestbrowsers/firefox:${FIREFOX_VERSION_MAJ}-${EB_VERSION}"
docker push "elastestbrowsers/firefox:${FIREFOX_VERSION_MAJ}"
docker push "elastestbrowsers/firefox:latest-${EB_VERSION}"
docker push "elastestbrowsers/firefox:latest"
docker push "elastestbrowsers/firefox:beta-${EB_VERSION}"
docker push "elastestbrowsers/firefox:beta"
docker push "elastestbrowsers/firefox:nightly-${EB_VERSION}"
docker push "elastestbrowsers/firefox:nightly"

if [ "$MODE" == "FULL" ]; then
  for FIREFOX_VERSION in "${FIREFOX_VERSIONS[@]}"; do
    FIREFOX_VERSION_MAJ="${FIREFOX_VERSION%%.*}"
    docker push "elastestbrowsers/firefox:${FIREFOX_VERSION_MAJ}-${EB_VERSION}"
  done

  for CHROME_VERSION in "${CHROME_VERSIONS[@]}"; do
    CHROME_VERSION_MAJ="${CHROME_VERSION%%.*}"
    docker push "elastestbrowsers/chrome:${CHROME_VERSION_MAJ}-${EB_VERSION}"
  done
fi

docker logout
