#!/bin/bash
UNAME=`uname`
if [ "$UNAME" == "Darwin" ]; then
    OS="darwin"
    PROCESSOR=`uname -p`
    if [ "$PROCESSOR" == "i386" ]; then
      ARCH="amd64"
    else
      ARCH="arm64"
    fi
else
    OS="linux"
    ARCH="amd64"
fi
VERSION="2.30.3"
INSTALLED_VERSION=`$WORKSPACE/op --version`
if [ "$INSTALLED_VERSION" != "$VERSION" ]; then
    echo "1Password CLI not installed yet. Installing..."
    curl -sSfLo op.zip "https://cache.agilebits.com/dist/1P/op2/pkg/v${VERSION}/op_${OS}_${ARCH}_v${VERSION}.zip"
    unzip -o op.zip -d op-dir
    mv -f op-dir/op $WORKSPACE/
    rm -r op.zip op-dir
fi