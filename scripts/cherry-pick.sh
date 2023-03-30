#!/bin/bash

# Get the branch name from command line argument
BRANCH_NAME=$1

# Set the branch names
RC_FIX_BRANCH="rc-$BRANCH_NAME"
DEVELOP_FIX_BRANCH="$BRANCH_NAME"

DEVELOP_BRANCH="develop"
RELEASE_CANDIDATE_BRANCH="release/candidate"

# Checkout the RC_FIX_BRANCH and get the hash of the latest commit
git checkout $RC_FIX_BRANCH
LAST_COMMIT=$(git log --format="%H" -n 1)

# Create and checkout the DEVELOP_FIX_BRANCH from the latest commit on the DEVELOP_BRANCH
git checkout -b $DEVELOP_FIX_BRANCH $LAST_COMMIT

# Find the commits that are unique to the RELEASE_CANDIDATE_BRANCH
RC_COMMITS=$(git log $RELEASE_CANDIDATE_BRANCH..$RC_FIX_BRANCH --format="%H")

# Cherry-pick the unique commits onto the DEVELOP_FIX_BRANCH
for COMMIT in $RC_COMMITS
do
    git cherry-pick --no-merges $COMMIT
done
