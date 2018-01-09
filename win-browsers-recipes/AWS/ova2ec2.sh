#!/bin/bash
set -e

# General data
DATE=$(date +%s)
export AWS_SECRET_ACCESS_KEY=
export AWS_ACCESS_KEY_ID=

# Download the image
OVA_FILE="MSEdge_-_Win10_preview.ova"
if [ ! -f "$OVA_FILE" ]; then
  curl 'https://az792536.vo.msecnd.net/vms/VMBuild_20171019/VirtualBox/MSEdge/MSEdge.Win10.VirtualBox.zip' -H 'Host: az792536.vo.msecnd.net' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:57.0) Gecko/20100101 Firefox/57.0' -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' -H 'Accept-Language: es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3' --compressed -H 'Referer: https://developer.microsoft.com/en-us/microsoft-edge/tools/vms/' -H 'Connection: keep-alive' -H 'Upgrade-Insecure-Requests: 1' -o MSEdge-Win10.zip
  unzip MSEdge-Win10.zip
  rename 's/ /_/g' ./*
  FILE_NAME=$(ls ./*ova)
  mv $FILE_NAME MSEdge_-_Win10_preview.ova
fi

# Upload OVA to S3
# Create bucket
aws s3api create-bucket --bucket ${DATE} --region eu-west-1 --create-bucket-configuration LocationConstraint=eu-west-1
# Upload the file
aws s3 cp MSEdge_-_Win10_preview.ova s3://${DATE}/windows.ova 

# Creating necessary roles and policy
cat > trust-policy.json <<-EOF
{
   "Version": "2012-10-17",
   "Statement": [
      {
         "Effect": "Allow",
         "Principal": { "Service": "vmie.amazonaws.com" },
         "Action": "sts:AssumeRole",
         "Condition": {
            "StringEquals":{
               "sts:Externalid": "vmimport"
            }
         }
      }
   ]
}
EOF
aws iam create-role --role-name vmimport --assume-role-policy-document file://trust-policy.json
sleep 5

cat > role-policy.json <<-EOF
{
   "Version": "2012-10-17",
   "Statement": [
      {
         "Effect": "Allow",
         "Action": [
            "s3:ListBucket",
            "s3:GetBucketLocation",
            "s3:FullAccess"
         ],
         "Resource": [
            "arn:aws:s3:::${DATE}"
         ]
      },
      {
         "Effect": "Allow",
         "Action": [
            "s3:GetObject"
         ],
         "Resource": [
            "arn:aws:s3:::${DATE}/*"
         ]
      },
      {
         "Effect": "Allow",
         "Action":[
            "ec2:ModifySnapshotAttribute",
            "ec2:CopySnapshot",
            "ec2:RegisterImage",
            "ec2:Describe*",
            "ec2:FullAccess"
         ],
         "Resource": "*"
      }
   ]
}
EOF
aws iam put-role-policy --role-name vmimport --policy-name vmimport --policy-document file://role-policy.json
sleep 5

cat > containers.json <<-EOF
[
  {
    "Description": "Windows 10 - MSEdge OVA",
    "Format": "ova",
    "UserBucket": {
        "S3Bucket": "${DATE}",
        "S3Key": "windows.ova"
    }
}]
EOF
sleep 5

aws ec2 import-image --description "Windows 10 - MSEdge OVA" --license-type BYOL --disk-containers file://containers.json
