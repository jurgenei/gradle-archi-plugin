# Multi-stage Dockerfile for Archi with plugins
FROM eclipse-temurin:21-jdk-jammy AS builder

ARG ARCHI_VERSION=5.4.0
ARG ARCHI_DOWNLOAD_URL=https://github.com/archimatetool/archi/releases/download/${ARCHI_VERSION}/Archi-${ARCHI_VERSION}-Linux64.zip

RUN apt-get update && apt-get install -y --no-install-recommends unzip wget && rm -rf /var/lib/apt/lists/*

WORKDIR /tmp
RUN wget -q "${ARCHI_DOWNLOAD_URL}" -O archi.zip && unzip -q archi.zip && rm archi.zip && mv Archi-*-Linux64 /opt/archi
COPY src/main/resources/archi/plugins/ /opt/archi/plugins/ 2>/dev/null || true

FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends xvfb unzip && rm -rf /var/lib/apt/lists/*
COPY --from=builder /opt/archi /opt/archi
ENV ARCHI_HOME=/opt/archi PATH="/opt/archi:${PATH}"
WORKDIR /workspace
ENTRYPOINT ["/opt/archi/Archi", "-consoleLog", "-nosplash"]

