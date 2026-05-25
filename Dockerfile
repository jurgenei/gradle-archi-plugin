# Multi-stage Dockerfile for Archi with plugins
FROM eclipse-temurin:21-jdk-jammy AS builder

ARG ARCHI_VERSION=5.9.0
ARG ARCHI_DOWNLOAD_URL=https://github.com/archimatetool/archi.io/releases/download/${ARCHI_VERSION}/Archi-Linux64-${ARCHI_VERSION}.tgz

RUN apt-get update && apt-get install -y --no-install-recommends tar wget && rm -rf /var/lib/apt/lists/*

WORKDIR /tmp
RUN wget -q "${ARCHI_DOWNLOAD_URL}" -O archi.tgz \
	&& mkdir -p /opt/archi \
	&& tar -xzf archi.tgz -C /opt/archi --strip-components=1 \
	&& rm archi.tgz
COPY src/main/resources/archi/plugins/ /opt/archi/plugins/

FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
	xvfb \
	unzip \
	libgtk-3-0 \
	libnss3 \
	libxss1 \
	libxtst6 \
	libxrender1 \
	libxi6 \
	libasound2 \
	libglib2.0-0 \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=builder /opt/archi /opt/archi
ENV ARCHI_HOME=/opt/archi PATH="/opt/archi:${PATH}"
WORKDIR /workspace
ENTRYPOINT ["/opt/archi/Archi", "-consoleLog", "-nosplash"]

