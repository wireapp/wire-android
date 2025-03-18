#!/usr/bin/env bash

s3cmd ls s3://z-lohika/android/candidate/ | tail -n 1 | cut -d$' ' -f7 | xargs -I {} sh -c "s3cmd get --force {} ~/Downloads/cand.apk"