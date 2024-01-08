#!/bin/bash -e

#
# Wire
# Copyright (C) 2024 Wire Swiss GmbH
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see http://www.gnu.org/licenses/.
#

function rotateFile {
    echo "Rotating /tmp/logcat.log on exit..."
    mv /tmp/logcat.log /tmp/logcat-$(date +%F-%T).log
}

trap rotateFile EXIT # exec this "log rotation" on bash script exit signal
touch /tmp/logcat.log
$HOME/Library/Android/sdk/platform-tools/adb logcat --pid=`$HOME/Library/Android/sdk/platform-tools/adb shell pidof -s com.wire.android.dev.debug` >> /tmp/logcat.log &
tail -f /tmp/logcat.log
