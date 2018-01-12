Start-Process powershell -Verb runAs
Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
choco.exe install -r --no-progress -y jdk8 --timeout 2700 --failonunfound --version 8.0.152
choco.exe install -r --no-progress -y selenium-all-drivers

$vncserver = new-object System.Net.WebClient
$vncserver.DownloadFile("https://www.tightvnc.com/download/2.8.8/tightvnc-2.8.8-gpl-setup-64bit.msi","C:\vncserver.msi")
$msifile = 'C:\vncserver.msi'
$arguments = ' /qn /l*v ./vnc-install.log SET_USEVNCAUTHENTICATION=1 VALUE_OF_USEVNCAUTHENTICATION=1 SET_PASSWORD=1 VALUE_OF_PASSWORD=secret SET_VIEWONLYPASSWORD=1 VALUE_OF_VIEWONLYPASSWORD=viewpass SET_USECONTROLAUTHENTICATION=1 VALUE_OF_USECONTROLAUTHENTICATION=1 SET_CONTROLPASSWORD=1 VALUE_OF_CONTROLPASSWORD=secret'
Start-Process `
  -file $msifile `
  -arg $arguments `
  -passthru | wait-process

$seleniumserver = new-object System.Net.WebClient
$seleniumserver.DownloadFile("http://selenium-release.storage.googleapis.com/3.8/selenium-server-standalone-3.8.1.jar", "C:\Users\IEUser\selenium-server-standalone.jar")

Add-Content c:\selenium.bat "@ECHO OFF"
Add-Content c:\selenium.bat "`nset JAVA_HOME=C:\Program files\Java\jdk1.8.0_152"
Add-Content c:\selenium.bat "`nset JDK_HOME=%JAVA_HOME%"
Add-Content c:\selenium.bat "`nset CLASSPATH=%JAVA_HOME%\lib;"
Add-Content c:\selenium.bat "`nset PATH=%PATH%;%JAVA_HOME%\bin;"
Add-Content c:\selenium.bat "`njava -Dwebdriver.edge.driver=`"C:\tools\selenium\MicrosoftWebDriver.exe`" -Dwebdriver.ie.driver=`"C:\tools\selenium\IEDriverServer.exe`" -jar C:\Users\IEUser\selenium-server-standalone.jar -host WINDOWS_IP -hub http://SELENIUM_IP:4444 -port 5556 -role node"

netsh Advfirewall set allprofiles state off

