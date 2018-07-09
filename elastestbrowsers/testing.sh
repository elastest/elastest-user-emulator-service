#!/bin/bash 
set -e

DOCKER_OPS="--rm -d --cap-add=SYS_ADMIN -p 4444:4444 -p 5900:5900"
LOG_RESULTS=output.log

> $LOG_RESULTS

# Firefox
for BROWSER_VERSION in latest nightly beta
do
	echo "* Testing firefox $BROWSER_VERSION"
	docker rm -f firefox || true
	docker run --name firefox $DOCKER_OPS elastestbrowsers/firefox:$BROWSER_VERSION
	sleep 5
	CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' firefox)
	RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"firefox","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
	docker stop firefox
	if [[ "$RES" == *200 ]] 
	then
		echo "Firefox $BROWSER_VERSION -- Ok!" | tee $LOG_RESULTS
	else
		echo "Firefox $BROWSER_VERSION -- Fail" | tee $LOG_RESULTS
		exit 1
	fi
done

# Chrome
for BROWSER_VERSION in latest unstable beta
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
		echo "Chrome $BROWSER_VERSION -- Ok!" | tee $LOG_RESULTS
	else
		echo "Crome $BROWSER_VERSION -- Fail" | tee $LOG_RESULTS
		exit 1
	fi
done

cat $LOG_RESULTS
