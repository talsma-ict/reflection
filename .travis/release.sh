#!/bin/bash

set -euo pipefail
set -x

#----------------------
# MAIN
#----------------------

# 1. Detect 'release-x.y.z' tags on develop and master branches
#    --> create 'release/x.y.z' branch to automatically perform the release.
# 2. Detect 'release/x.y.z' branch
#    --> set version to 'x.y.z'
#    --> build & test
#    --> tag 'vx.y.z' (on release/x.y.z)

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
    echo "Not releasing from pull-request ${TRAVIS_PULL_REQUEST}."
elif [ ! "${TRAVIS_BRANCH}" =~ ^develop|master$ ]; then
    echo "Not releasing from branch ${TRAVIS_BRANCH}."
elif [[ ! "${TRAVIS_TAG}" =~ ^release\-[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9]+)*(\+[A-Za-z0-9\.\-]+)?$ ]]; then
    echo "No 'release-x.y.z' tag found, skipping release."
else
    echo "Performing ${TRAVIS_TAG} from branch ${TRAVIS_BRANCH}."
fi
