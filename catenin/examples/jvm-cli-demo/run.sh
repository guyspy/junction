#!/bin/bash
# JVM CLI Demo Run Script
# This script builds and runs the demo without Gradle's progress bar interference

echo "Building JVM CLI Demo..."
../../../gradlew :catenin:examples:jvm-cli-demo:installDist --console=plain

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Starting game..."
echo ""

# Set JAVA_HOME to use the same Java version that Gradle uses
if [ -z "$JAVA_HOME" ]; then
    echo "Warning: JAVA_HOME not set. Using system Java which may cause version conflicts."
    echo "Consider setting JAVA_HOME to point to Java 21 for best compatibility."
fi

# Run using the generated script (no Gradle interference)
./build/install/jvm-cli-demo/bin/jvm-cli-demo