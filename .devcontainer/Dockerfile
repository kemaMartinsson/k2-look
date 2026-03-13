# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

ARG USERNAME=vscode

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl wget unzip zip git openssh-server \
        lib32z1 lib32ncurses6 lib32stdc++6 \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js LTS
RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install sdkman + Java 21 (system-wide at /usr/local/sdkman)
ENV SDKMAN_DIR=/usr/local/sdkman
RUN curl -s "https://get.sdkman.io" | SDKMAN_DIR=/usr/local/sdkman bash \
    && bash -c "source /usr/local/sdkman/bin/sdkman-init.sh && sdk install java 21.0.5-ms" \
    && chown -R ${USERNAME}:${USERNAME} /usr/local/sdkman

ENV JAVA_HOME=/usr/local/sdkman/candidates/java/current
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Android SDK command-line tools
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
         -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mv /tmp/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip \
    && chown -R ${USERNAME}:${USERNAME} ${ANDROID_SDK_ROOT}

# Accept licenses and install SDK packages as the container user
RUN sudo -E -u ${USERNAME} bash -c "\
    yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --licenses && \
    ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager \
        'platform-tools' \
        'platforms;android-34' \
        'build-tools;34.0.0'"

# Configure sshd
RUN mkdir -p /run/sshd

# Persist environment variables into the vscode user shell profile
RUN echo 'export SDKMAN_DIR=/usr/local/sdkman' >> /home/${USERNAME}/.bashrc \
    && echo 'source /usr/local/sdkman/bin/sdkman-init.sh' >> /home/${USERNAME}/.bashrc \
    && echo 'export ANDROID_SDK_ROOT=/opt/android-sdk' >> /home/${USERNAME}/.bashrc \
    && echo 'export PATH="$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools"' >> /home/${USERNAME}/.bashrc

# Default workdir
WORKDIR /workspaces
