#!/bin/bash
set -e

CMDLIST="expect";
for i in $CMDLIST
do
  command -v $i >/dev/null && continue || { echo "ERROR: $i command not found."; exit 1; }
done

WINDOWS_IP=
SELENIUM_IP=

sed "s/WINDOWS_IP/$WINDOWS_IP/g;s/SELENIUM_IP/$SELENIUM_IP/g" script_template.ps1 > script.ps1 || exit 1

sed "s/WINDOWS_IP/$WINDOWS_IP/g" copy_powershell_script.temp > copy_powershell_script.exp
sed "s/WINDOWS_IP/$WINDOWS_IP/g" run_powershell_script.temp > run_powershell_script.exp

chmod +x copy_powershell_script.exp run_powershell_script.exp

./copy_powershell_script.exp
./run_powershell_script.exp

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
