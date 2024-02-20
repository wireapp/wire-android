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

# start from features directory
cd features

# change template in files
git grep -rl template . | xargs sed -i 's#template#lolo#g'

# change template in package names and imports
find src/ -type f -name '*.kt' | xargs sed -i "" 's#template#lolo#g'

# change folder names
find . -name "*template*" | awk '{a=$1; gsub(/template/,"lolo"); printf "mv \"%s\" \"%s\"\n", a, $1}' | sh
