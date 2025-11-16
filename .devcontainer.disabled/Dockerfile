# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

# Install dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        openjdk-11-jdk \
        git \
        wget \
        unzip \
        curl \
        build-essential \
        lib32z1 \
        lib32ncurses6 \
        lib32stdc++6 \
        python3 \
        python3-pip \
        sudo \
        adb \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK command line tools
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/emulator"

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    cd /tmp && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip && \
    rm cmdline-tools.zip && \
    mv cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_SDK_ROOT --licenses && \
    $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_SDK_ROOT "platform-tools" "platforms;android-30" "build-tools;30.0.3" "emulator"

# Set up user (for VS Code devcontainers)
ARG USERNAME=vscode
RUN usermod -aG plugdev $USERNAME

# Default workdir
WORKDIR /workspace
