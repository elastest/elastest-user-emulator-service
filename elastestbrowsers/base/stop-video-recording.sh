#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Send the SIGINT signal to FFmpeg, which means "stop processing"
pkill --signal SIGINT --exact ffmpeg || true

# Wait for FFmpeg to finish
while pgrep --exact ffmpeg >/dev/null; do
  sleep 0.500s
done

echo "FFmpeg recording is now finished"
