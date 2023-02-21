#!/usr/bin/env bash

# Source env for rustup
source "$HOME/.cargo/env"

# Change working directory to core-crypto
cd core-crypto

# Add targets for android
rustup target add x86_64-linux-android aarch64-linux-android armv7-linux-androideabi i686-linux-android

# Change working directory to crypto-ffi
cd crypto-ffi

# Build android core-crypto library
cargo make "copy-android-resources"
cd ../kotlin

./gradlew :jvm:publishToMavenLocal
./gradlew :android:publishToMavenLocal


#mvn install:install-file -Dfile=../kotlin/android/build/outputs/aar/android-release.aar \
#    -DgroupId=com.wire \
#    -DartifactId=core-crypto-android \
#    -Dversion=0.6.0-rc.3 \
#    -Dpackaging=aar \
#    -DlocalRepositoryPath=/home/android-agent/.m2/repository

#cd ../kotlin

#../../gradlew assemble
