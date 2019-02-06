#!/bin/bash 
set -eu -o pipefail

MODE=${MODE:-NIGHTLY}
DOCKER_OPS="--rm -d --cap-add=SYS_ADMIN -p 4444:4444 -p 5900:5900"
LOG_RESULTS=output.log
> $LOG_RESULTS
# Load old releases versions
. browsers_oldreleases
EXIT_RES=0

# Firefox
for BROWSER_VERSION in latest-${EB_VERSION} nightly-${EB_VERSION} beta-${EB_VERSION}
do
	echo "**********************************"
	echo "* Testing firefox $BROWSER_VERSION"
	echo "**********************************"
	docker rm -f firefox || true
	docker run --name firefox $DOCKER_OPS elastestbrowsers/firefox:$BROWSER_VERSION
	sleep 5
	CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' firefox)
	RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"firefox","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
	docker stop firefox
	if [[ "$RES" == *200 ]] 
	then
		echo "Firefox $BROWSER_VERSION -- Ok!" | tee -a $LOG_RESULTS
	else
		echo "Firefox $BROWSER_VERSION -- Fail" | tee -a $LOG_RESULTS
		RES=1
	fi
done

# Testing Old Version
if [ "$MODE" == "FULL" ]; then
	for BROWSER_VERSION in $FIREFOX_OLD_VERSIONS
	do
		echo "**********************************"
		echo "* Testing firefox $BROWSER_VERSION"
		echo "**********************************"
		docker rm -f firefox || true
		docker run --name firefox $DOCKER_OPS elastestbrowsers/firefox:$BROWSER_VERSION-${EB_VERSION}
		sleep 5
		CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' firefox)
		RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"firefox","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
		docker stop firefox
		if [[ "$RES" == *200 ]] 
		then
			echo "Firefox $BROWSER_VERSION-${EB_VERSION} -- Ok!" | tee -a $LOG_RESULTS
		else
			echo "Firefox $BROWSER_VERSION-${EB_VERSION} -- Fail" | tee -a $LOG_RESULTS
			EXIT_RES=1
		fi
	done
fi

# Chrome
for BROWSER_VERSION in latest-${EB_VERSION} unstable-${EB_VERSION} beta-${EB_VERSION}
do
	echo "*********************************"
	echo "* Testing chrome $BROWSER_VERSION"
	echo "*********************************"
	docker rm -f chrome || true
	docker run --name chrome $DOCKER_OPS elastestbrowsers/chrome:$BROWSER_VERSION
	sleep 5
	CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' chrome)
	RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"chrome","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
	docker stop chrome
	if [[ "$RES" == *200 ]]
	then
		echo "Chrome $BROWSER_VERSION -- Ok!" | tee -a $LOG_RESULTS
	else
		echo "Chrome $BROWSER_VERSION -- Fail" | tee -a $LOG_RESULTS
		EXIT_RES=1
	fi
done

# Testing Old Version
if [ "$MODE" == "FULL" ]; then
	for BROWSER_VERSION in $CHROME_OLD_VERSIONS
	do
		V=$(echo $BROWSER_VERSION | cut -d"." -f1,2)
		echo "**********************************"
		echo "* Testing Chrome $BROWSER_VERSION"
		echo "**********************************"
		docker rm -f chrome || true
		docker run --name chrome $DOCKER_OPS elastestbrowsers/chrome:$V-${EB_VERSION}
		sleep 5
		CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' chrome)
		RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"chrome","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
		docker stop chrome
		if [[ "$RES" == *200 ]] 
		then
			echo "Chrome $BROWSER_VERSION-${EB_VERSION} -- Ok!" | tee -a $LOG_RESULTS
		else
			echo "Chrome $BROWSER_VERSION-${EB_VERSION} -- Fail" | tee -a $LOG_RESULTS
			RES=1
		fi
	done
fi

cat $LOG_RESULTS

exit $EXIT_RES

