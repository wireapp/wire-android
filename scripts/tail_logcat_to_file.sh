#!/bin/bash -e

function rotateFile {
    echo "Rotating /tmp/logcat.log on exit..."
    mv /tmp/logcat.log /tmp/logcat-$(date +%F-%T).log
}

trap rotateFile EXIT # exec this "log rotation" on bash script exit signal
touch /tmp/logcat.log
$HOME/Library/Android/sdk/platform-tools/adb logcat --pid=`$HOME/Library/Android/sdk/platform-tools/adb shell pidof -s com.wire.android.dev.debug` >> /tmp/logcat.log &
tail -f /tmp/logcat.log
