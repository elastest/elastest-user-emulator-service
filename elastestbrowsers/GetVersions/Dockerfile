FROM ubuntu:18.04

RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        gnupg \
        software-properties-common \
        wget \
 && rm -rf /var/lib/apt/lists/*

VOLUME /workdir

ADD ./get_versions.sh /usr/local/bin/get_versions.sh

ENTRYPOINT [ "/usr/local/bin/get_versions.sh" ]

CMD /usr/local/bin/get_versions.sh
