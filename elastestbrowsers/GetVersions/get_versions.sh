#!/bin/bash -x
set -e

OUTPUT=/workdir
VERSIONS_FILE=$OUTPUT/versions.txt

# Firefox
apt-get update
FIREFOX_PKG=$(apt-cache madison firefox | head -n1 | awk '{ print $3 }')
FIREFOX_VER=$(echo $FIREFOX_PKG | cut -d"." -f1)

# Firefox Beta
add-apt-repository --yes ppa:mozillateam/firefox-next && apt-get update
FIREFOX_BETA_PKG=$(apt-cache madison firefox | head -n1 | awk '{ print $3 }')
FIREFOX_BETA_VER=$(echo $FIREFOX_BETA_PKG | cut -d"." -f1)

# Firefox Nightly
add-apt-repository --yes ppa:ubuntu-mozilla-daily/ppa && apt-get update
FIREFOX_NIGHTLY_PKG=$(apt-cache madison firefox-trunk | head -n1 | awk '{print $3'})
FIREFOX_NIGHTLY_VER=$(echo $FIREFOX_NIGHTLY_PKG | cut -d"." -f1)

# Crome
wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
        echo 'deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main' > /etc/apt/sources.list.d/google.list && \
        apt-get update 
CHROME_PKG=$(apt-cache madison google-chrome-stable | head -n1 | awk '{print $3}')
CHROME_VER=$(echo $CHROME_PKG | cut -d"." -f1)

# Chrome beta
CHROME_BETA_PKG=$(apt-cache madison google-chrome-beta | head -n1 | awk '{print $3}')
CHROME_BETA_VER=$(echo $CHROME_BETA_PKG | cut -d"." -f1)

# Chrome unstable
CHROME_UNSTABLE_PKG=$(apt-cache madison google-chrome-unstable | head -n1 | awk '{print $3}')
CHROME_UNSTABLE_VER=$(echo $CHROME_UNSTABLE_PKG | cut -d"." -f1)

# Selenoid driver
SELENOID_VERSION=$(curl --silent "https://api.github.com/repos/aerokube/selenoid/releases/latest" | grep '"tag_name":' |  sed -E 's/.*"([^"]+)".*/\1/')
wget -O $OUTPUT/selenoid_linux_amd64 https://github.com/aerokube/selenoid/releases/download/${SELENOID_VERSION}/selenoid_linux_amd64
chmod +x $OUTPUT/selenoid_linux_amd64

# OUTPUT
echo FIREFOX_PKG=$FIREFOX_PKG > $VERSIONS_FILE
echo FIREFOX_VER=$FIREFOX_VER >> $VERSIONS_FILE
echo FIREFOX_BETA_VER=$FIREFOX_BETA_VER >> $VERSIONS_FILE
echo FIREFOX_NIGHTLY_VER=$FIREFOX_NIGHTLY_VER >> $VERSIONS_FILE
echo CHROME_PKG=$CHROME_PKG >> $VERSIONS_FILE
echo CHROME_VER=$CHROME_VER >> $VERSIONS_FILE
echo CHROME_BETA_VER=$CHROME_BETA_VER >> $VERSIONS_FILE
echo CHROME_UNSTABLE_VER=$CHROME_UNSTABLE_VER >> $VERSIONS_FILE
echo SELENOID_VERSION=$SELENOID_VERSION >> $VERSIONS_FILE

cat $VERSIONS_FILE

chmod -R 777 $OUTPUT
