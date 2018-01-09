#!/bin/bash
set -e

# Download the image
OVA_FILE="MSEdge_-_Win10_preview.ova"
if [ ! -f "$OVA_FILE" ]; then
  curl 'https://az792536.vo.msecnd.net/vms/VMBuild_20171019/VirtualBox/MSEdge/MSEdge.Win10.VirtualBox.zip' -H 'Host: az792536.vo.msecnd.net' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:57.0) Gecko/20100101 Firefox/57.0' -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' -H 'Accept-Language: es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3' --compressed -H 'Referer: https://developer.microsoft.com/en-us/microsoft-edge/tools/vms/' -H 'Connection: keep-alive' -H 'Upgrade-Insecure-Requests: 1' -o MSEdge-Win10.zip
  unzip MSEdge-Win10.zip
  rename 's/ /_/g' ./*
  FILE_NAME=$(ls ./*ova)
  mv $FILE_NAME MSEdge_-_Win10_preview.ova
fi

tar xf MSEdge_-_Win10_preview.ova
rename 's/ /_/g' ./*

qemu-img convert -f vmdk -O qcow2 MSEdge_-_Win10-disk001.vmdk MSEdge_-_Win10-disk001.qcow2

