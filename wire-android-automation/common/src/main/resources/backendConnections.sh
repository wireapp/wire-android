#!/bin/bash
#
# This file generates a JSON file out of all entries in the Test Automation vault of 1Password that are in the category "Server"
#

VAULT="Test Automation"
JSONFILE="backendConnections.json"

# Create tempfile and make sure it is deleted after bash script exited
TEMPFILE=$(mktemp "${TMPDIR:-/tmp/}$(basename $0).XXXXXXX")
trap 'rm -f -- "$TEMPFILE"' EXIT

# Check if binary is installed system-wide or in workspace
if [ -n "$WORKSPACE" ]; then
  BINARY=$WORKSPACE/op
else
  BINARY=op
fi

$BINARY item list --vault "Test Automation" --categories Server --format=json | $BINARY item get --format=json --reveal - > $TEMPFILE

if [ ! -s "$TEMPFILE" ]; then
  echo "Unable to get backend connections via op"
  exit 1
fi

echo "" >> $TEMPFILE
echo "[" > $JSONFILE
awk '{
    if ($0 == "}") {
        getline nextline
        if (nextline == "{") {
            print "},{"
            next
        } else {
            print "}"
            next
        }
    } else {
        print $0
    }
}' $TEMPFILE >> $JSONFILE
echo "]" >> $JSONFILE
