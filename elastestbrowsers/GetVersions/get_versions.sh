#!/bin/bash -x
set -e

OUTPUT=/workdir
VERSIONS_FILE=$OUTPUT/versions.txt

# Firefox
apt-get update
FIREFOX_PKG=$(apt-cache madison firefox | head -n1 | awk '{ print $3 }')
FIREFOX_VER=$(echo $FIREFOX_PKG | cut -d"." -f1)

# Firefox Beta
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 0AB215679C571D1C8325275B9BDB3D89CE49EC21 && \
        echo "deb http://ppa.launchpad.net/mozillateam/firefox-next/ubuntu xenial main" >> /etc/apt/sources.list.d/firefox-beta.list && \
        apt-get update -qqy
FIREFOX_BETA_PKG=$(apt-cache madison firefox | head -n1 | awk '{ print $3 }')
FIREFOX_BETA_VER=$(echo $FIREFOX_BETA_PKG | cut -d"." -f1)

# Firefox Nightly
add-apt-repository -y ppa:ubuntu-mozilla-daily/ppa && apt-get update
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
wget -O $OUTPUT/selenoid_linux_amd64 https://github.com/aerokube/selenoid/releases/download/1.3.9/selenoid_linux_amd64
chmod +x $OUTPUT/selenoid_linux_amd64

# Gecko driver
GECKO_RELEASE_URL=https://api.github.com/repos/mozilla/geckodriver/releases/latest
GECKO=$(curl $GECKO_RELEASE_URL | jq --raw-output '.tag_name' | cut -d"v" -f2)
wget -O $OUTPUT/geckodriver.tar.gz https://github.com/mozilla/geckodriver/releases/download/v$GECKO/geckodriver-v$GECKO-linux64.tar.gz
tar xvzf $OUTPUT/geckodriver.tar.gz -C $OUTPUT
rm -Rfv $OUTPUT/geckodriver.tar.gz

# Chrome driver
CHROME_DRIVER_VER=$(curl https://chromedriver.storage.googleapis.com/LATEST_RELEASE)
wget -O $OUTPUT/chromedriver.zip https://chromedriver.storage.googleapis.com/$CHROME_DRIVER_VER/chromedriver_linux64.zip
unzip $OUTPUT/chromedriver.zip -d $OUTPUT
rm -Rfv $OUTPUT/chromedriver.zip

# OUTPUT
echo FIREFOX_PKG=$FIREFOX_PKG > $VERSIONS_FILE
echo FIREFOX_VER=$FIREFOX_VER >> $VERSIONS_FILE
echo FIREFOX_BETA_VER=$FIREFOX_BETA_VER >> $VERSIONS_FILE
echo FIREFOX_NIGHTLY_VER=$FIREFOX_NIGHTLY_VER >> $VERSIONS_FILE
echo CHROME_PKG=$CHROME_PKG >> $VERSIONS_FILE
echo CHROME_VER=$CHROME_VER >> $VERSIONS_FILE
echo CHROME_BETA_VER=$CHROME_BETA_VER >> $VERSIONS_FILE
echo CHROME_UNSTABLE_VER=$CHROME_UNSTABLE_VER >> $VERSIONS_FILE
echo GECKO_VERSION=$GECKO >> $VERSIONS_FILE
echo CHROME_DRIVER_VER=$CHROME_DRIVER_VER >> $VERSIONS_FILE

cat $VERSIONS_FILE

chmod -R 777 $OUTPUT
