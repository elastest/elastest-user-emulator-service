FROM ubuntu:18.04

# Set Image Labels
ARG GIT_COMMIT=unspecified
LABEL git_commit=$GIT_COMMIT

ARG COMMIT_DATE=unspecified
LABEL commit_date=$COMMIT_DATE

ARG VERSION=unspecified
LABEL version=$VERSION

RUN apt-get update -qq

# Install dependencies
RUN apt-get install -y vim
RUN apt-get install -y build-essential git
RUN apt-get install -y jq
RUN apt install -y ffmpeg
RUN apt-get install -y imagemagick

ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt-get install -y python python-setuptools python-dev python-tk python-pip
RUN pip install --upgrade pip
RUN pip install numpy scipy matplotlib notebook pandas sympy nose scikit-learn scikit-image h5py sureal

# ElasTest WebRTC QoE Meter Repo
RUN git clone https://github.com/elastest/elastest-webrtc-qoe-meter
ENV ELASTEST_WEBRTC_QOE_METER_PATH=/elastest-webrtc-qoe-meter
ENV ELASTEST_WEBRTC_QOE_METER_SCRIPTS_PATH=/elastest-webrtc-qoe-meter/scripts

# VQMT Repo
RUN git clone https://github.com/Rolinh/VQMT
ENV VQMT_PATH=/VQMT

# Netflix VMAF repo
RUN git clone --depth 1 https://github.com/Netflix/vmaf.git vmaf
WORKDIR vmaf/
ENV PYTHONPATH=/vmaf/python/src:/vmaf:$PYTHONPATH
ENV PATH=/vmaf:/vmaf/wrapper:$PATH
ENV VMAF_PATH=/vmaf
RUN make
WORKDIR /root/
