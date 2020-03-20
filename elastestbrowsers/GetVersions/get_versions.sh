#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Settings
# ========

WORKDIR="/workdir"
OUTPUT_FILE="$WORKDIR/versions.txt"



# Get browser details
# ===================

function get_apt_version {
  local PACKAGE_NAME="${1:-}"
  [[ -z "$PACKAGE_NAME" ]] && return 1
  apt-cache --no-all-versions show "$PACKAGE_NAME" | awk '/Version:/{ print $2 }'
}



# Firefox
# -------

apt-get update
FIREFOX_PKG="$(get_apt_version firefox)"
FIREFOX_VER="${FIREFOX_PKG%%.*}"

add-apt-repository --yes ppa:mozillateam/firefox-next && apt-get update
FIREFOX_BETA_PKG="$(get_apt_version firefox)"
FIREFOX_BETA_VER="${FIREFOX_BETA_PKG%%.*}"

add-apt-repository --yes ppa:ubuntu-mozilla-daily/ppa && apt-get update
FIREFOX_NIGHTLY_PKG="$(get_apt_version firefox-trunk)"
FIREFOX_NIGHTLY_VER="${FIREFOX_NIGHTLY_PKG%%.*}"



# Crome
# -----

wget -q -O - "https://dl.google.com/linux/linux_signing_key.pub" | apt-key add -
echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >/etc/apt/sources.list.d/google-chrome.list
apt-get update

CHROME_PKG="$(get_apt_version google-chrome-stable)"
CHROME_VER="${CHROME_PKG%%.*}"

CHROME_BETA_PKG="$(get_apt_version google-chrome-beta)"
CHROME_BETA_VER="${CHROME_BETA_PKG%%.*}"

CHROME_UNSTABLE_PKG="$(get_apt_version google-chrome-unstable)"
CHROME_UNSTABLE_VER="${CHROME_UNSTABLE_PKG%%.*}"



# Selenoid
# --------

SELENOID_VERSION="$(wget -q -O - "https://api.github.com/repos/aerokube/selenoid/releases/latest" | grep '"tag_name":' |  sed -E 's/.*"([^"]+)".*/\1/')"
wget -O "$WORKDIR/selenoid_linux_amd64" "https://github.com/aerokube/selenoid/releases/download/${SELENOID_VERSION}/selenoid_linux_amd64"
chmod +x "$WORKDIR/selenoid_linux_amd64"



# Write output file
# =================

tee "$OUTPUT_FILE" >/dev/null <<EOF
FIREFOX_PKG=$FIREFOX_PKG
FIREFOX_VER=$FIREFOX_VER
FIREFOX_BETA_PKG=$FIREFOX_BETA_PKG
FIREFOX_BETA_VER=$FIREFOX_BETA_VER
FIREFOX_NIGHTLY_PKG=$FIREFOX_NIGHTLY_PKG
FIREFOX_NIGHTLY_VER=$FIREFOX_NIGHTLY_VER
CHROME_PKG=$CHROME_PKG
CHROME_VER=$CHROME_VER
CHROME_BETA_PKG=$CHROME_BETA_PKG
CHROME_BETA_VER=$CHROME_BETA_VER
CHROME_UNSTABLE_PKG=$CHROME_UNSTABLE_PKG
CHROME_UNSTABLE_VER=$CHROME_UNSTABLE_VER
SELENOID_VERSION=$SELENOID_VERSION
EOF

echo "Done! Output file contents:"
cat "$OUTPUT_FILE"
