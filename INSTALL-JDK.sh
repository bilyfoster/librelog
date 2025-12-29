#!/bin/bash
# Script to install JDK - run this manually with sudo

echo "Installing OpenJDK 21 JDK..."
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk-headless

echo "Verifying installation..."
javac -version

echo "Setting JAVA_HOME..."
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo "Add this to your ~/.bashrc:"
echo "export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
echo "export PATH=\$JAVA_HOME/bin:\$PATH"
