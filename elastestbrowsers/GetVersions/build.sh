#!/bin/bash -x
set -e

docker build -t elastestbrowsers/utils-get_browsers_version:1.0 .
if [ ! -z $DOCKERHUB_USERNAME ]; then
	docker login -u $DOCKERHUB_USERNAME -p $DOCKERHUB_PASS
	docker push elastestbrowsers/utils-get_browsers_version:1.0
	docker logout
fi