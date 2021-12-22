FROM openjdk:11

RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get update && \
    apt-get install -yq libc6 libstdc++6 zlib1g libncurses5 build-essential libssl-dev ruby ruby-dev --no-install-recommends docker.io vim git && \
    apt-get clean

RUN gem install bundler

# Cleaning
RUN apt-get clean

ARG USER=android-agent
ARG USER_ID=1000
ARG GROUP_ID=1000
RUN useradd -m ${USER} --uid=${USER_ID}
USER ${USER_ID}:${GROUP_ID}
WORKDIR /home/${USER}
ENV HOME /home/${USER}

# Download and untar Android SDK tools
RUN mkdir -p /home/${USER}/android-sdk-linux && \
    wget https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip -O tools.zip && \
    unzip tools.zip -d /home/${USER}/android-sdk && \
    rm tools.zip

# Download and untar Android NDK tools
ENV ANDROID_NDK_HOME /home/${USER}/android-ndk
ENV ANDROID_NDK_VERSION r22b
RUN mkdir /home/${USER}/android-ndk-tmp && \
    cd /home/${USER}/android-ndk-tmp && \
    wget -q https://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip && \
# uncompress
    unzip -q android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip && \
# move to its final location
    mv ./android-ndk-${ANDROID_NDK_VERSION} ${ANDROID_NDK_HOME} && \
# remove temp dir
    cd ${ANDROID_NDK_HOME} && \
    rm -rf /home/${USER}/android-ndk-tmp

# Download latest cmdline-tools to fix the sdkmanager jdk11 incompatibilities
RUN mkdir /home/${USER}/cmdline-tools-tmp && \
    cd /home/${USER}/cmdline-tools-tmp && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip && \
# uncompress
    unzip commandlinetools-linux-7583922_latest.zip && \
# hackytrick as google is stupid
    mkdir /home/${USER}/android-sdk/cmdline-tools/ && \
# now copy the cmdlone-tools folder as the actual tools folder
    mv cmdline-tools /home/${USER}/android-sdk/cmdline-tools/tools && \
# remove the temp folder
    cd /home/${USER} && \
    rm -rf /home/${USER}/cmdline-tools-tmp

# Set environment variable
ENV ANDROID_HOME /home/${USER}/android-sdk
ENV ANDROID_SDK=${ANDROID_HOME}
ENV PATH ${ANDROID_HOME}/cmdline-tools/tools:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${ANDROID_HOME}/cmdline-tools/tools/bin:$ANDROID_NDK_HOME:$PATH

#define the values to install/setup via the sdk manager
ARG BUILD_TOOLS_VERSION=30.0.2
ARG PLATFORMS_VERSION=android-29
ARG ARCHITECTURE=x86

# Make license agreement
RUN yes | $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager --licenses

# Update and install using sdkmanager
RUN $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager "tools" "platform-tools" "build-tools;${BUILD_TOOLS_VERSION}" "platforms;${PLATFORMS_VERSION}" "system-images;${PLATFORMS_VERSION};default;${ARCHITECTURE}" "extras;android;m2repository" "extras;google;m2repository"

# uncomment if you need to run the docker image standalone for testing purpose
#CMD tail -f /dev/null
