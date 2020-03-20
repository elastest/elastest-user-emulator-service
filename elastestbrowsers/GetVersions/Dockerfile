FROM ubuntu:18.04

RUN apt-get update && apt-get install --yes --no-install-recommends \
        gnupg \
        software-properties-common \
        wget \
 && rm -rf /var/lib/apt/lists/*

VOLUME /workdir

ADD ./get_versions.sh /usr/local/bin/get_versions.sh

ENTRYPOINT [ "/usr/local/bin/get_versions.sh" ]

CMD /usr/local/bin/get_versions.sh
