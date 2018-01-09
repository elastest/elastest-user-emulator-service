#!/bin/bash 
set -e

INSTANCE_NAME="Elastest_win10"

# check if there is any previous instance
if [ ! -z "$(vboxmanage list vms | grep "$INSTANCE_NAME")" ]; then
  vboxmanage controlvm "$INSTANCE_NAME" poweroff
  vboxmanage unregistervm "$INSTANCE_NAME" --delete
fi

CMDLIST="VBoxManage ./copy_powershell_script.exp ./run_powershell_script.exp expect curl";
for i in $CMDLIST
do
  command -v $i >/dev/null && continue || { echo "ERROR: $i command not found."; exit 1; }
done

echo "#######################################"
echo " STARTING DEPLOYMENT AT [$(date +%H:%M:%S)]"
echo "#######################################"

# Get VitualBox Version
VBOX_VERSION=$(VBoxManage --version)

# Download the image
OVA_FILE="MSEdge_-_Win10_preview.ova"
if [ ! -f "$OVA_FILE" ]; then
  curl 'https://az792536.vo.msecnd.net/vms/VMBuild_20171019/VirtualBox/MSEdge/MSEdge.Win10.VirtualBox.zip' -H 'Host: az792536.vo.msecnd.net' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:57.0) Gecko/20100101 Firefox/57.0' -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' -H 'Accept-Language: es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3' --compressed -H 'Referer: https://developer.microsoft.com/en-us/microsoft-edge/tools/vms/' -H 'Connection: keep-alive' -H 'Upgrade-Insecure-Requests: 1' -o MSEdge-Win10.zip
  unzip MSEdge-Win10.zip
  rename 's/ /_/g' ./*
  FILE_NAME=$(ls ./*ova)
  mv $FILE_NAME MSEdge_-_Win10_preview.ova
fi


# Import the instance
if [[ $VBOX_VERSION == *"5.0"* ]]; then 
  vboxmanage import MSEdge_-_Win10_preview.ova --vsys 0 --ostype WindowsNT_64 --vmname "$INSTANCE_NAME"
else
  vboxmanage import MSEdge_-_Win10_preview.ova --vsys 0 --vmname "$INSTANCE_NAME"
fi

# Enable ports to access Selenium Server, SSH and VNC
vboxmanage modifyvm "$INSTANCE_NAME" --natpf1 "Selenium,tcp,,5556,,5556" || exit 1
vboxmanage modifyvm "$INSTANCE_NAME" --natpf1 "SSH,tcp,,2222,,22" || exit 1
vboxmanage modifyvm "$INSTANCE_NAME" --natpf1 "VNC,tcp,,5900,,5900" || exit 1

# Starting instance
vboxmanage startvm "$INSTANCE_NAME" --type headless

echo "Waiting 300 seconds for windows to be up and ready"
secs=300
while [ $secs -gt 0 ]; do
  echo -ne "_$secs\033[0K\r"
  sleep 1
  : $((secs--))
done

# Getting network data
MY_IP=$(ip route get 8.8.8.8 | head -1 | cut -d' ' -f8)
sed "s/HOST_IP/$MY_IP/g" script_template.ps1 > script.ps1 || exit 1

# Copy PowerShell script
./copy_powershell_script.exp

# Run the PowerShell script
./run_powershell_script.exp 

# Launch Selenium hub
SELENIUM_CONTAINER=$(docker ps | grep selenium-hub)
if [ ! -z "$SELENIUM_CONTAINER" ]; then
  docker rm -f selenium-hub
fi
docker run -d --rm -p 4444:4444 --name selenium-hub selenium/hub

# Generating launching script
cat >run_selenium_server.exp<<EOF
#!/usr/bin/expect -f

set timeout -1

spawn ssh -o "StrictHostKeyChecking no" "IEUser@$localhost" -p2222
expect "IEUser@$localhost\'s password: "
send "Passw0rd!\r"
expect -- "-sh-4.1\$ "
send "powershell \"C:\\\\selenium.bat\"\r"
expect -- "-sh-4.1\$ "
send "exit\r"
EOF

echo "#######################################"
echo " FINISHING DEPLOYMENT AT [$(date +%H:%M:%S)]"
echo "#######################################"

exit 0
