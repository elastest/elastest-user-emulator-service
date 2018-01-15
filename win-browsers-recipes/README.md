Scripts And Docs To Run Tests On Windows Browsers
=================================================

## Introduction

In this repo you'll find the necessary scripts to deploy a Dockerized version of Selenium Hub and a Windows 10 with Edge and IExplorer.

The idea behind those scripts is to allow you to test your applications on Windows environment.

We've worked on three different providers: *AWS*, *VirtualBox* and *OpenStack*, and it basically has four steps:

1. Download the OVA from Microsoft Web Site.
2. Register the image with the provider.
3. Provisioning the image with Java and Selenium Drivers.
4. And finally join windows instance to the hub.

Please, choose the provider best fit your needs. We assume you have enough knowledge of the provider you choose.

## Amazon Web Services (AWS)

![alt text](https://github.com/elastest/elastest-user-emulator-service/blob/master/win-browsers-recipes/images/aws.png "Diagram")

We assume you have a valid AWS Account and a Linux Box with aws cli installed. In this scripts you **must** fill your AWS credentials, that is:

```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

The idea is to deploy two EC2 instances, one with Windows and the second one with Selenium Hub.

### Registering the OVA as an AMI

First of all, we need to register the OVA as an Amazon Machine Image. To do so, just run this script:

```
ova2ec2.sh
```

from the AWS folder. Please, remind to set up the credentials.

This process takes a while, like an hour or so. Please be patient. When the process has finished you can find the AMI in AWS Dashboard. You'll need the ID in the next step.

### Running CloudFormation

With the value you got before, plus the **credetials** and a valid RSA key to access the Selenium Hub instance when it's running you can configure the next script *deploy_windows_AWS.sh*. Something like this:

```
export AWS_SECRET_ACCESS_KEY="FcJnX....vS"
export AWS_ACCESS_KEY_ID="AKI....D"
EC2_KEYNAME=mykey.pem
EC2_AMIID=ami-7ab52003
```

Next, there are two *yaml* files in the folder you have to upload to AWS S3. After that, write the complete URL in the script. Something like:

```
SELENIUM_HUB_YAML=https://s3-eu-west-1.amazonaws.com/YOUR_BUCKET_NAME/CFN-SeleniumHub.yaml 
WINDOWS_YAML=https://s3-eu-west-1.amazonaws.com/YOUR_BUCKET_NAME/CFN-SeleniumHub.yaml
```

replace *YOUR_BUCKET_NAME* for an apropiate value, and then run the script:

```
deploy_windows_AWS.sh
```

By the end of the process you will find a script 

```
run_selenium_server.exp
```

which will allow you to launch Selenium in the Windows Machine to register it in the hub. Also you can check the **Output** Tab in **AWS CloudFormation** Dashboard to see the URL to access the Hub with a browser.

## VirtualBox

![alt text](https://github.com/elastest/elastest-user-emulator-service/blob/master/win-browsers-recipes/images/virtualbox.png "Diagram")

We've been working with Ubuntu 16.04 OpenSource version of VirtualBox. It's `5.0.40_Ubuntur115130` right now. We know the script works fine with VirtualBox 5.1.

We assume that you have a machine with VirtualBox and Docker installed. So in this scenario Selenium Hub and Windows are running on the same machine.

To deploy in VirtualBox run this command from the machine with VirtualBox and Docker installed:

``` 
deploy_windows_VB.sh
```

from VirtualBox folder.

The script will download the OVA for you, will register it on VirtualBox and will provisioned it. By the end of the process you will find a script: 

```
run_selenium_server.exp
```

which will allow you to launch Selenium in the Windows Machine to register it in the hub. Also you can check this URL:

```
http://localhost:4444
```

to access Selenium Hub. Alternatively, you can access from any instance whithin your LAN by changing *localhost* for the appropiate *IP*.

## OpenStack

![alt text](https://github.com/elastest/elastest-user-emulator-service/blob/master/win-browsers-recipes/images/openstack.png "Diagram")

We assume you have a running OpenStack installation. We've been working with Ocata but it should works version independent.

We are running the commands from the controller node.

### Registering the image

First of all, you need to convert from *ova* to *qcow2*, so run this script:

```
ova2qcow2.sh
```

from the *OpenStack* folder.

Next, register the image in Glance. First, load the OpenStack credentials as **environment variables**:

```
. user-openrc
```

and then:

```
openstack image create "Elastest-Win10-image" \
  --file MSEdge_-_Win10-disk001.qcow2 \
  --disk-format qcow2 \
  --container-format bare \
  --property hw_disk_bus=ide \
  --property hw_vif_model=e1000 \
  --public
```

You'll need the image ID for the next step.

### Provisioning the instance

Now, you need to provide some information to the script `deploy_windows_OS.sh`:

```
KEYPAIR= key pair to access Selenium Hub instance when it's ready.
WINIMAGEID= ID of windows image you create previously
UBUNTUIMAGE= ID of your ubuntu xenial image
NETWORKID= ID of the network you want to attach your instances
INSTANCEFLAVOR= Size of Ubuntu xenial instance
WINFLAVOR= Size of Windows instance
```

and then run the script:

```
deploy_windows_OS.sh
```

By the end of this process you will be able to register the instance in the hub by running:

```
run_selenium_server.exp
```

Also, the script will output the URL to access the Selenium Console.