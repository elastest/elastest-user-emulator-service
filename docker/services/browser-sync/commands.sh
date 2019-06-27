#!/bin/bash

# Example docker run --rm --name browser-sync -e "BROWSER_SYNC_OPTIONS=--proxy 'https://elastest.io' --open 'external'" --network elastest_elastest elastest/eus-browsersync 

export START_COMMAND="browser-sync start $(echo $BROWSER_SYNC_OPTIONS)"; 
echo "#!/bin/bash" >> /source/start.sh; 
echo $START_COMMAND >> /source/start.sh; 
cat /source/start.sh; 
echo ""
chmod 777 /source/start.sh; 
./start.sh
