#!/usr/bin/env bash

if [ "$RUN_STATIC_CODE_ANALYSIS" = true ]; then
    echo "Running Static Code Analysis"
   ./gradlew staticCodeAnalysis
fi

if [ "$RUN_APP_UNIT_TESTS" = true ] ; then
    echo "Running App Unit Tests"
    ./gradlew runUnitTests
fi

if [ "$RUN_APP_ACCEPTANCE_TESTS" = true ] ; then
    echo "Running Acceptance Tests"
    ./gradlew runAcceptanceTests
fi

buildOption=''
if [ "$BUILD_WITH_STACKTRACE" = true ] ; then
    buildOption="--stacktrace "
    echo "Stacktrace option enabled"
fi

if [ "$CLEAN_PROJECT_BEFORE_BUILD" = true ] ; then
    echo "Cleaning the Project"
    ./gradlew clean
else
    echo "Cleaning the project will be skipped"
fi

if [ "$BUILD_CLIENT" = true ] ; then
    echo "Compiling the client with Flavor:${CUSTOM_FLAVOR} and BuildType:${BUILD_TYPE}"
    ./gradlew ${buildOption}assemble${FLAVOR_TYPE}${BUILD_TYPE} -PisFDroidRelease=true
else
    echo "Building the client will be skipped"
fi

if [ "$SIGN_APK" = true ] ; then
    echo "Signing APK with given details"
    clientVersion=$(sed -ne "s/.*ANDROID_CLIENT_MAJOR_VERSION = \"\([^']*\)\"/\1/p" buildSrc/src/main/kotlin/Dependencies.kt)
   /home/android-agent/android-sdk/build-tools/35.0.0/apksigner sign --ks ${HOME}/wire-android/${KEYSTORE_PATH} --ks-key-alias ${KEYSTORE_KEY_NAME} --ks-pass pass:${KSTOREPWD} --key-pass pass:${KEYPWD} "${HOME}/wire-android/app/build/outputs/apk/wire-${CUSTOM_FLAVOR,,}-${BUILD_TYPE,,}-${clientVersion}${PATCH_VERSION}.apk"
else
   echo "Apk will not be signed by the builder script"
fi
