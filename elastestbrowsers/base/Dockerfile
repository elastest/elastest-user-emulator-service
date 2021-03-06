#
# Base image for Elastest Browsers
#

FROM ubuntu:16.04

ARG DEBIAN_FRONTEND=noninteractive
ARG EB_VERSION
ARG GIT_URL
ARG GIT_COMMIT
ARG BUILD_DATE

LABEL maintainer fede diaz nordri@gmail.com \
      org.label-schema.vendor="Elastest" \
      org.label-schema.url="https://elastest.io" \
      org.label-schema.name="Elastest browsers" \
      org.label-schema.description="Optimal resource util" \    
      org.label-schema.version=$EB_VERSION \
      org.label-schema.vcs-url=$GIT_URL \
      org.label-schema.vcs-ref=$GIT_COMMIT \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.docker.schema-version="1.0" 

########################################################
# Base system
########################################################
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      curl \
      xvfb \
      libgconf-2-4 \
      libnss3-dev \
      x11vnc \
      bsdtar \
      python \
      net-tools \
      wget \
      bzip2 \
      libgtk-3-0 \
      sudo \
      software-properties-common \
      xorg \
      iproute2 \
      pulseaudio && \
    apt-get -y autoremove && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

########################################################
# Install websokify and noVNC
########################################################
RUN curl -O https://bootstrap.pypa.io/get-pip.py && \
    python3 get-pip.py && \
    pip3 install --no-cache-dir \
        setuptools && \
    pip3 install -U https://github.com/novnc/websockify/archive/master.tar.gz && \
    mkdir /usr/local/noVNC && \
    curl -s -L https://github.com/x11vnc/noVNC/archive/master.tar.gz | \
         bsdtar zxf - -C /usr/local/noVNC --strip-components 1 && \
    rm -rf /tmp/* /var/tmp/*

########################################################
# Fluxbox
########################################################
RUN apt-get update && \
  apt-get install --yes fluxbox && \
    apt-get -y autoremove && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

########################################################
# ffmpeg
########################################################
RUN add-apt-repository -y ppa:mc3man/xerus-media && \
    apt-get update && \
    apt-get install --yes ffmpeg && \
    apt-get -y autoremove && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

########################################################
# Some files
########################################################
# Creating user and setting the environment
RUN adduser --uid 1000 --disabled-password --gecos "" --home /home/ubuntu ubuntu
RUN echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/ubuntu
RUN mkdir -p /home/ubuntu/.fluxbox
ADD lastwallpaper /home/ubuntu/.fluxbox/lastwallpaper
ADD overlay /home/ubuntu/.fluxbox/overlay
ADD elastest-wallpaper.png /home/ubuntu/.fluxbox/elastest-wallpaper.png
RUN chown -R ubuntu.ubuntu /home/ubuntu/.fluxbox
# Recording script
ADD start-video-recording.sh /usr/local/bin/start-video-recording.sh
ADD stop-video-recording.sh /usr/local/bin/stop-video-recording.sh 

########################################################
# Entrypoint
########################################################
ADD entrypoint.sh /usr/local/bin/entrypoint.sh
