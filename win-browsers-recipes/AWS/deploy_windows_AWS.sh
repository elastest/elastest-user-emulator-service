#!/bin/bash 

set -e

export AWS_SECRET_ACCESS_KEY=
export AWS_ACCESS_KEY_ID=

EC2_KEYNAME=
EC2_AMIID=
SELENIUM_HUB_YAML=
WINDOWS_YAML=

DATE_SUFFIX=$(date +%s)

aws cloudformation create-stack \
  --stack-name elastest-seleniumhub-$DATE_SUFFIX \
	--template-url $SELENIUM_HUB_YAML \
  --parameters '[{"ParameterKey": "KeyName", "ParameterValue": "'$EC2_KEYNAME'"}]'

echo "Waiting for the Selenium Hub instance to be available"
aws cloudformation wait stack-create-complete --stack-name elastest-seleniumhub-$DATE_SUFFIX
if [[ $? == 255 ]]; then
  exit 1
fi

for entry in $(aws cloudformation list-exports | jq --compact-output '.Exports | .[]'); do
  if [ "$(echo $entry | jq --compact-output '.Name ' | tr -d '"')" == "SeleniumHubPublicIP" ]; then
    SELENIUMHUBPUBLICIP=$(echo $entry | jq --compact-output '.Value ' | tr -d '"')
  fi
done

aws cloudformation create-stack \
  --stack-name elastest-win10-edge-$DATE_SUFFIX \
  --template-url $WINDOWS_YAML \
  --parameters '[{"ParameterKey": "AMIID", "ParameterValue": "'$EC2AMIID'"}, {"ParameterKey": "SeleniumHubPublicIP", "ParameterValue": "'$SELENIUMHUBPUBLICIP'"}]'

echo "Waiting 600 seconds for windows to be up and ready"
secs=600
while [ $secs -gt 0 ]; do
  echo -ne "_$secs\033[0K\r"
  sleep 1
  : $((secs--))
done

for entry in $(aws cloudformation list-exports | jq --compact-output '.Exports | .[]'); do
  if [ "$(echo $entry | jq --compact-output '.Name ' | tr -d '"')" == "WindowsPublicIP" ]; then
    WINDOWSPUBLICIP=$(echo $entry | jq --compact-output '.Value ' | tr -d '"')
  fi
done

echo "Launching Selenium Server"
cat >run_selenium_server.exp<<EOF
#!/usr/bin/expect -f

set timeout -1

spawn ssh -o "StrictHostKeyChecking no" "IEUser@$WINDOWSPUBLICIP" 
expect "IEUser@$WINDOWSPUBLICIP\'s password: "
send "Passw0rd!\r"
expect -- "-sh-4.1\$ "
send "powershell \"C:\\\\selenium.bat\"\r"
expect -- "-sh-4.1\$ "
send "exit\r"
EOF
chmod +x run_selenium_server.exp