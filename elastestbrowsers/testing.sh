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

DOCKER_OPS="--rm -d --cap-add=SYS_ADMIN -p 4444:4444 -p 5900:5900"
LOG_RESULTS=output.log
> $LOG_RESULTS
EXIT_RES=0

# Firefox
for FIREFOX_VERSION in latest-${EB_VERSION} nightly-${EB_VERSION} beta-${EB_VERSION}
do
	echo "**********************************"
	echo "* Testing firefox $FIREFOX_VERSION"
	echo "**********************************"
	docker rm -f firefox || true
	docker run --name firefox $DOCKER_OPS elastestbrowsers/firefox:$FIREFOX_VERSION
	sleep 5
	CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' firefox)
	RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"firefox","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
	docker stop firefox
	if [[ "$RES" == *200 ]]
	then
		echo "Firefox $FIREFOX_VERSION -- Ok!" | tee -a $LOG_RESULTS
	else
		echo "Firefox $FIREFOX_VERSION -- Fail" | tee -a $LOG_RESULTS
		RES=1
	fi
done

# Testing Old Version
if [ "$MODE" == "FULL" ]; then
	for FIREFOX_VERSION in "${FIREFOX_VERSIONS[@]}"
	do
		echo "**********************************"
		echo "* Testing firefox $FIREFOX_VERSION"
		echo "**********************************"
		docker rm -f firefox || true
		docker run --name firefox $DOCKER_OPS elastestbrowsers/firefox:$FIREFOX_VERSION-${EB_VERSION}
		sleep 5
		CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' firefox)
		RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"firefox","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
		docker stop firefox
		if [[ "$RES" == *200 ]]
		then
			echo "Firefox $FIREFOX_VERSION-${EB_VERSION} -- Ok!" | tee -a $LOG_RESULTS
		else
			echo "Firefox $FIREFOX_VERSION-${EB_VERSION} -- Fail" | tee -a $LOG_RESULTS
			EXIT_RES=1
		fi
	done
fi

# Chrome
for CHROME_VERSION in latest-${EB_VERSION} unstable-${EB_VERSION} beta-${EB_VERSION}
do
	echo "*********************************"
	echo "* Testing chrome $CHROME_VERSION"
	echo "*********************************"
	docker rm -f chrome || true
	docker run --name chrome $DOCKER_OPS elastestbrowsers/chrome:$CHROME_VERSION
	sleep 5
	CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' chrome)
	RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"chrome","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
	docker stop chrome
	if [[ "$RES" == *200 ]]
	then
		echo "Chrome $CHROME_VERSION -- Ok!" | tee -a $LOG_RESULTS
	else
		echo "Chrome $CHROME_VERSION -- Fail" | tee -a $LOG_RESULTS
		EXIT_RES=1
	fi
done

# Testing Old Version
if [ "$MODE" == "FULL" ]; then
	for CHROME_VERSION in "${CHROME_OLD_VERSIONS[@]}"
	do
		V=$(echo $CHROME_VERSION | cut -d"." -f1,2)
		echo "**********************************"
		echo "* Testing Chrome $CHROME_VERSION"
		echo "**********************************"
		docker rm -f chrome || true
		docker run --name chrome $DOCKER_OPS elastestbrowsers/chrome:$V-${EB_VERSION}
		sleep 5
		CONTAINER_IP=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{ .IPAddress}}{{end}}' chrome)
		RES=$(curl --silent -X POST -d '{"desiredCapabilities":{"browserName":"chrome","version":"","platform":"ANY"}}' --write-out "%{http_code}\\n" http://$CONTAINER_IP:4444/wd/hub/session)
		docker stop chrome
		if [[ "$RES" == *200 ]]
		then
			echo "Chrome $CHROME_VERSION-${EB_VERSION} -- Ok!" | tee -a $LOG_RESULTS
		else
			echo "Chrome $CHROME_VERSION-${EB_VERSION} -- Fail" | tee -a $LOG_RESULTS
			RES=1
		fi
	done
fi

echo ""
echo "## TEST RESULTS ##"
cat $LOG_RESULTS

exit $EXIT_RES
