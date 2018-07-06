#!/bin/bash -x
set -eu -o pipefail

docker build -t elastestbrowsers/utils-get_browsers_version:$1 .
docker push elastestbrowsers/utils-get_browsers_version:$1
