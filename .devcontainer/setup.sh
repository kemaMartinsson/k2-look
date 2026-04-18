#!/bin/bash
set -e

echo "Installing Android development tools..."

# Install dependencies
sudo apt-get update
sudo apt-get install -y wget unzip openjdk-11-jdk android-tools-adb

# Download Android command-line tools
mkdir -p /tmp/android-sdk
cd /tmp/android-sdk
wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip -q commandlinetools-linux-9477386_latest.zip

# Setup Android SDK
export ANDROID_HOME=/opt/android-sdk
sudo mkdir -p $ANDROID_HOME/cmdline-tools
sudo mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest

# Accept licenses
yes | sudo $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses || true

# Install platform tools
sudo $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-30" "build-tools;30.0.3"

# Set environment variables
echo 'export ANDROID_HOME=/opt/android-sdk' | sudo tee -a /etc/profile
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' | sudo tee -a /etc/profile

# Cleanup
cd /
rm -rf /tmp/android-sdk

echo "Android development tools installed successfully!"
