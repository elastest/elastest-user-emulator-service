#
# Base image for Elastest Browsers
#

FROM phusion/baseimage:0.10.0
LABEL maintainer fede diaz nordri@gmail.com

ARG DOCKER_LANG=en_US
ARG DOCKER_TIMEZONE=America/New_York

ENV LANG=$DOCKER_LANG.UTF-8 \
    LANGUAGE=$DOCKER_LANG:UTF-8 \
    LC_ALL=$DOCKER_LANG.UTF-8

WORKDIR /tmp

ARG DEBIAN_FRONTEND=noninteractive

########################################################
# Install some required system tools and packages for X Windows
########################################################
RUN locale-gen $LANG && \
    dpkg-reconfigure -f noninteractive locales && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        man \
        sudo \
        rsync \
        bsdtar \
        net-tools \
        inetutils-ping \
        dos2unix \
        python \
        xserver-xorg-video-dummy \
        x11-xserver-utils \
        x11-utils \
        xterm \
        gnome-themes-standard \
        gtk2-engines-pixbuf \
        gtk2-engines-murrine \
        ttf-ubuntu-font-family \
        xfonts-base xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic \
        mesa-utils \
        libgl1-mesa-dri \
        pulseaudio \
        xvfb \
        bzip2 \
        wget \
        x11vnc && \
        \
    apt-get -y autoremove && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

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
# Install websokify and noVNC
########################################################
RUN curl -O https://bootstrap.pypa.io/get-pip.py && \
    python2 get-pip.py && \
    pip2 install --no-cache-dir \
        setuptools && \
    pip2 install -U https://github.com/novnc/websockify/archive/master.tar.gz && \
    mkdir /usr/local/noVNC && \
    curl -s -L https://github.com/x11vnc/noVNC/archive/master.tar.gz | \
         bsdtar zxf - -C /usr/local/noVNC --strip-components 1 && \
    rm -rf /tmp/* /var/tmp/*

# Install x11vnc from source
# Install x-related to compile x11vnc from source code.
# https://bugs.launchpad.net/ubuntu/+source/x11vnc/+bug/1686084
# https://gist.github.com/mangoliou/407d6a39a60d128610c20c4143f39f0d
RUN apt-get update && \
   apt-get install -y libxtst-dev libssl-dev libjpeg-dev build-essential && \
   \
   mkdir -p /tmp/x11vnc-0.9.14 && \
   curl -s -L http://x11vnc.sourceforge.net/dev/x11vnc-0.9.14-dev.tar.gz | \
       bsdtar zxf - -C /tmp/x11vnc-0.9.14 --strip-components 1 && \
   cd /tmp/x11vnc-0.9.14 && \
   ./configure --prefix=/usr/local CFLAGS='-O2 -fno-stack-protector -Wall' && \
   make && \
   make install && \
   apt-get -y remove libxtst-dev libssl-dev libjpeg-dev build-essential build-essential && \
   apt-get -y autoremove && \
   rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

########################################################
# Customization for user and location
########################################################
# Set up user so that we do not run as root
ENV DOCKER_USER=ubuntu \
    DOCKER_SHELL=/bin/bash

ENV DOCKER_GROUP=$DOCKER_USER \
    DOCKER_HOME=/home/$DOCKER_USER \
    HOME=/home/$DOCKER_USER

# Change the default timezone to $DOCKER_TIMEZONE
# Run ldconfig so that /usr/local/lib etc. are in the default
# search path for dynamic linker
RUN useradd -m -s $DOCKER_SHELL -G sudo,docker_env $DOCKER_USER && \
    echo "$DOCKER_USER:docker" | chpasswd && \
    echo "$DOCKER_USER ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers && \
    echo "$DOCKER_TIMEZONE" > /etc/timezone && \
    ln -s -f /usr/share/zoneinfo/$DOCKER_TIMEZONE /etc/localtime && \
    ldconfig

ADD image/etc /etc
ADD image/usr /usr
ADD image/home $DOCKER_HOME

RUN touch $DOCKER_HOME/.sudo_as_admin_successful && \
    mkdir -p $DOCKER_HOME/shared && \
    mkdir -p $DOCKER_HOME/.ssh && \
    mkdir -p $DOCKER_HOME/.log && touch $DOCKER_HOME/.log/vnc.log && \
    echo "export NO_AT_BRIDGE=1" >> $DOCKER_HOME/.profile && \
    ln -s -f .config/mozilla $HOME/.mozilla && \
    echo "[ ! -f $HOME/WELCOME -o -z \"\$DISPLAY\" ] || cat $HOME/WELCOME" \
        >> $DOCKER_HOME/.profile && \
    chown -R $DOCKER_USER:$DOCKER_GROUP $DOCKER_HOME

WORKDIR $DOCKER_HOME

USER root
