#!/bin/bash -x
set -e

MODE=${MODE:-NIGHTLY}

WORKDIR=$PWD/workdir
. $WORKDIR/versions.txt
. browsers_oldreleases

docker push elastestbrowsers/utils-x11-base:1.1
  
docker push elastestbrowsers/chrome:latest
docker push elastestbrowsers/chrome:$CHROME_VER
docker push elastestbrowsers/chrome:beta
docker push elastestbrowsers/chrome:unstable

docker push elastestbrowsers/firefox:latest
docker push elastestbrowsers/firefox:$FIREFOX_VER
docker push elastestbrowsers/firefox:beta
docker push elastestbrowsers/firefox:nightly

if [ "$MODE" == "FULL" ]; then
	for V in $FIREFOX_OLD_VERSIONS
	do
		docker push elastestbrowsers/firefox:$V
	done

	OLDVERSIONS=""
	for VERSION in $CHROME_OLD_VERSIONS
	do
		OLDVERSIONS+="$(echo $VERSION | cut -d"." -f1,2) "
	done
	for V in $OLDVERSIONS
	do
		docker push elastestbrowsers/chrome:$V
	done
fi
  
docker logout
