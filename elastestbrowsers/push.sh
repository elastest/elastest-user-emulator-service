#!/bin/bash -x
set -eu -o pipefail

WORKDIR=$PWD/workdir
. $WORKDIR/versions.txt
. browsers_oldreleases

docker push elastestbrowsers/utils-x11-base:${EB_VERSION}
  
docker push elastestbrowsers/chrome:latest-${EB_VERSION}
docker push elastestbrowsers/chrome:latest
docker push elastestbrowsers/chrome:${CHROME_VER}-${EB_VERSION}
docker push elastestbrowsers/chrome:${CHROME_VER}
docker push elastestbrowsers/chrome:beta-${EB_VERSION}
docker push elastestbrowsers/chrome:beta
docker push elastestbrowsers/chrome:unstable-${EB_VERSION}
docker push elastestbrowsers/chrome:unstable

docker push elastestbrowsers/firefox:latest-${EB_VERSION}
docker push elastestbrowsers/firefox:latest
docker push elastestbrowsers/firefox:${FIREFOX_VER}-${EB_VERSION}
docker push elastestbrowsers/firefox:${FIREFOX_VER}
docker push elastestbrowsers/firefox:beta-${EB_VERSION}
docker push elastestbrowsers/firefox:beta
docker push elastestbrowsers/firefox:nightly-${EB_VERSION}
docker push elastestbrowsers/firefox:nightly

if [ "$MODE" == "FULL" ]; then
	for V in $FIREFOX_OLD_VERSIONS
	do
		docker push elastestbrowsers/firefox:$V-${EB_VERSION}
	done

	OLDVERSIONS=""
	for VERSION in $CHROME_OLD_VERSIONS
	do
		OLDVERSIONS+="$(echo $VERSION | cut -d"." -f1,2) "
	done
	for V in $OLDVERSIONS
	do
		docker push elastestbrowsers/chrome:$V-${EB_VERSION}
	done
fi
  
docker logout
