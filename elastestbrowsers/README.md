# elastestbrowsers

## Content

1. Base: Base X11 image with all needed packages

2. Chrome/Firefox: Specific browsers versions

3. GetVersion: Tool to obtain browsers/drivers versions

## Procedures

To generate the [utils-get_browsers_version](https://hub.docker.com/r/elastestbrowsers/utils-get_browsers_version) image, run:

```
cd GetVersions/
./build.sh 4 false
cd ..
```

First argument is the version number, and second argument decides whether to try pushing the image to Docker Hub (`true`) or not (`false`).

To generate all browser images, run:

```
EB_VERSION=2.1.0 ./build_containers.sh
```

With `EB_VERSION` set to the version number that should be used to tag *ElastestBrowsers* Docker images.
