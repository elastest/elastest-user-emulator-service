#!/bin/bash
set -e

KEYPAIR=
WINIMAGEID=
UBUNTUIMAGE=
NETWORKID=
INSTANCEFLAVOR=
WINFLAVOR=

CMDLIST="expect";
for i in $CMDLIST
do
  command -v $i >/dev/null && continue || { echo "ERROR: $i command not found."; exit 1; }
done

openstack stack create -t HOT-SeleniumHub.yaml --parameter \
  KeyName=$KEYPAIR\; \
  Image=$UBUNTUIMAGE\; \
  NetworkId=$NETWORKID\; \
  InstanceFlavor=$INSTANCEFLAVOR \
  SeleniumHubInstance

SELENIUM_IP=$(openstack stack output show SeleniumHubInstance Public_IP | grep output_value | awk '{ print $4 }')

openstack stack create -t HOT-WindowsTesting.yaml --parameter \
  ImageId=$WINIMAGEID\; \
  NetworkId=$NETWORKID\; \
  WinInstanceFlavor=$WINFLAVOR \
  WindowsInstance

echo "Waiting 600 seconds for windows to be up and ready"
secs=600
while [ $secs -gt 0 ]; do
  echo -ne "_$secs\033[0K\r"
  sleep 1
  : $((secs--))
done

WINDOWS_IP=$(openstack stack output show WindowsInstance Public_IP | grep output_value | awk '{ print $4 }')

sed "s/WINDOWS_IP/$WINDOWS_IP/g;s/SELENIUM_IP/$SELENIUM_IP/g" script_template.ps1 > script.ps1 || exit 1
sed "s/WINDOWS_IP/$WINDOWS_IP/g" copy_powershell_script.temp > copy_powershell_script.exp
sed "s/WINDOWS_IP/$WINDOWS_IP/g" run_powershell_script.temp > run_powershell_script.exp

chmod +x copy_powershell_script.exp run_powershell_script.exp

# Provisioning...
./copy_powershell_script.exp
./run_powershell_script.exp

echo "###############################################"
echo "You can click this url to open Selenium Console"
echo "http://$SELENIUM_IP:4444"
echo "###############################################"

cat >run_selenium_server.exp<<EOF
#!/usr/bin/expect -f

set timeout -1

spawn ssh -o "StrictHostKeyChecking no" "IEUser@$WINDOWS_IP" 
expect "IEUser@$WINDOWS_IP\'s password: "
send "Passw0rd!\r"
expect -- "-sh-4.1\$ "
send "powershell \"C:\\\\selenium.bat\"\r"
expect -- "-sh-4.1\$ "
send "exit\r"
EOF
chmod +x run_selenium_server.exp
