FROM psharkey/novnc

# Set Image Labels
ARG GIT_COMMIT=unspecified
LABEL git_commit=$GIT_COMMIT

ARG COMMIT_DATE=unspecified
LABEL commit_date=$COMMIT_DATE

ARG VERSION=unspecified
LABEL version=$VERSION 

COPY novnc/vnc-autofocus.html /root/noVNC
COPY novnc/novnc.sh /

RUN sed -i '/http:\/\/dl-6.alpinelinux.org\/alpine\/edge\/testing/d' /etc/apk/repositories

RUN echo "http://dl-2.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories; \
echo "http://dl-3.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories; \
echo "http://dl-4.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories; \
echo "http://dl-5.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories

RUN apk update upgrade && apk add ffmpeg build-base python python-dev py-pip curl

RUN pip install vnc2flv

RUN echo selenoid > passwd_file
