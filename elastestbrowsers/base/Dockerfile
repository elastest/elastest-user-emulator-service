#
# Base image for Elastest Browsers
#

FROM ubuntu:18.04

ARG EB_VERSION
ARG GIT_URL
ARG GIT_COMMIT
ARG BUILD_DATE

# Disable Apt warnings for running in non-interactive mode
ARG DEBIAN_FRONTEND=noninteractive
ARG APT_KEY_DONT_WARN_ON_DANGEROUS_USAGE=1

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
# Packages:
# - gnupg: for `apt-key`.
# - procps: for `pgrep`, `pkill`.
# - software-properties-common: for `add-apt-repository`.
#
# Notes:
# - Disable "autospawn" feature of PulseAudio: it should be explicitly started
#   by our entrypoint script, and not implicitly by other processes.
RUN apt-get update && apt-get install --yes --no-install-recommends \
        bsdtar \
        bzip2 \
        curl \
        gnupg \
        iproute2 \
        libgconf-2-4 \
        libgtk-3-0 \
        libnss3-dev \
        net-tools \
        procps \
        pulseaudio \
        python3 \
        python3-distutils \
        software-properties-common \
        sudo \
        wget \
        x11vnc \
        xorg \
        xvfb xauth \
 && rm -rf /var/lib/apt/lists/* \
 && sed -r -i "s/^;?\s*autospawn\s*=.*/autospawn = no/" /etc/pulse/client.conf

########################################################
# Install websokify and noVNC
########################################################
RUN curl -O "https://bootstrap.pypa.io/get-pip.py" \
 && python3 get-pip.py \
 && pip3 install --no-cache-dir --upgrade \
        setuptools \
 && pip3 install --no-cache-dir --upgrade \
        https://github.com/novnc/websockify/archive/master.tar.gz \
 && mkdir -p /usr/local/noVNC \
 && curl -s -L "https://github.com/x11vnc/noVNC/archive/master.tar.gz" \
        | bsdtar zxf - -C /usr/local/noVNC --strip-components 1

########################################################
# Fluxbox
########################################################
# Packages:
# - fluxbox: to have a Window Manager inside the X11 server.
# - hsetroot: to allow Fluxbox setting the desktop background.
RUN apt-get update && apt-get install --yes --no-install-recommends \
        fluxbox hsetroot \
 && rm -rf /var/lib/apt/lists/*

########################################################
# ffmpeg
########################################################
# RUN add-apt-repository -y ppa:mc3man/xerus-media && \
#     apt-get update && \
#     apt-get install --yes ffmpeg && \
#     apt-get -y autoremove && \
#     rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
RUN apt-get update && apt-get install --yes --no-install-recommends \
        ffmpeg \
 && rm -rf /var/lib/apt/lists/*

########################################################
# Some files
########################################################
# Create "ubuntu" user that will run PulseAudio, and set the environment
RUN adduser --system --group --disabled-password --shell /bin/bash ubuntu \
 && echo "ubuntu ALL=(ALL) NOPASSWD:ALL" >/etc/sudoers.d/ubuntu \
 && mkdir -p /home/ubuntu/.fluxbox
COPY elastest-wallpaper.png lastwallpaper overlay /home/ubuntu/.fluxbox/
RUN chown -R ubuntu:ubuntu /home/ubuntu/.fluxbox
# Recording script
COPY start-video-recording.sh stop-video-recording.sh /usr/local/bin/

########################################################
# Entrypoint
########################################################
COPY entrypoint.sh /usr/local/bin/
