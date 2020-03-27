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
FIREFOX_VERSION="$(get_apt_version firefox)"
FIREFOX_VERSION_MAJ="${FIREFOX_VERSION%%.*}"

add-apt-repository --yes ppa:mozillateam/firefox-next && apt-get update
FIREFOX_BETA_VERSION="$(get_apt_version firefox)"
FIREFOX_BETA_VERSION_MAJ="${FIREFOX_BETA_VERSION%%.*}"

add-apt-repository --yes ppa:ubuntu-mozilla-daily/ppa && apt-get update
FIREFOX_NIGHTLY_VERSION="$(get_apt_version firefox-trunk)"
FIREFOX_NIGHTLY_VERSION_MAJ="${FIREFOX_NIGHTLY_VERSION%%.*}"



# Crome
# -----

wget --quiet -O - "https://dl.google.com/linux/linux_signing_key.pub" | apt-key add -
echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >/etc/apt/sources.list.d/google-chrome.list
apt-get update

CHROME_VERSION="$(get_apt_version google-chrome-stable)"
CHROME_VERSION_MAJ="${CHROME_VERSION%%.*}"

CHROME_BETA_VERSION="$(get_apt_version google-chrome-beta)"
CHROME_BETA_VERSION_MAJ="${CHROME_BETA_VERSION%%.*}"

CHROME_UNSTABLE_VERSION="$(get_apt_version google-chrome-unstable)"
CHROME_UNSTABLE_VERSION_MAJ="${CHROME_UNSTABLE_VERSION%%.*}"



# Selenoid
# --------

SELENOID_VERSION="$(wget --quiet -O - "https://api.github.com/repos/aerokube/selenoid/releases/latest" | grep '"tag_name":' |  sed -E 's/.*"([^"]+)".*/\1/')"
wget --no-verbose -O "$WORKDIR/selenoid_linux_amd64" "https://github.com/aerokube/selenoid/releases/download/${SELENOID_VERSION}/selenoid_linux_amd64"
chmod +x "$WORKDIR/selenoid_linux_amd64"



# Write output file
# =================

tee "$OUTPUT_FILE" >/dev/null <<EOF
FIREFOX_VERSION=$FIREFOX_VERSION
FIREFOX_VERSION_MAJ=$FIREFOX_VERSION_MAJ
FIREFOX_BETA_VERSION=$FIREFOX_BETA_VERSION
FIREFOX_BETA_VERSION_MAJ=$FIREFOX_BETA_VERSION_MAJ
FIREFOX_NIGHTLY_VERSION=$FIREFOX_NIGHTLY_VERSION
FIREFOX_NIGHTLY_VERSION_MAJ=$FIREFOX_NIGHTLY_VERSION_MAJ
CHROME_VERSION=$CHROME_VERSION
CHROME_VERSION_MAJ=$CHROME_VERSION_MAJ
CHROME_BETA_VERSION=$CHROME_BETA_VERSION
CHROME_BETA_VERSION_MAJ=$CHROME_BETA_VERSION_MAJ
CHROME_UNSTABLE_VERSION=$CHROME_UNSTABLE_VERSION
CHROME_UNSTABLE_VERSION_MAJ=$CHROME_UNSTABLE_VERSION_MAJ
SELENOID_VERSION=$SELENOID_VERSION
EOF

echo "Done! Output file contents:"
cat "$OUTPUT_FILE"
