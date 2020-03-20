#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Arguments
# =========

BUILD_VERSION="${1:-test}"
DOCKER_PUSH="${2:-false}"



# Run Docker commands

docker build \
  --tag "elastestbrowsers/utils-get_browsers_version:${BUILD_VERSION}" \
  .

if [[ "$DOCKER_PUSH" == "true" ]]; then
  docker push \
    "elastestbrowsers/utils-get_browsers_version:${BUILD_VERSION}"
fi
