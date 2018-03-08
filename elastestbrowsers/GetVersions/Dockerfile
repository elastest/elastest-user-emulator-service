# Browsers versions
FROM phusion/baseimage:0.10.0

RUN \
    apt-get update && \
    apt-get install --yes --no-install-recommends \
      wget \
      software-properties-common \
      curl \
      jq \
      unzip

VOLUME /workdir

ADD ./get_versions.sh /usr/local/bin/get_versions.sh
ENTRYPOINT [ "/usr/local/bin/get_versions.sh" ]
CMD /usr/local/bin/get_versions.sh