#!/bin/bash

set -euo pipefail
set -x

is_semantic_version() {
    # Test whether the first argument matches a semantic version, see https://semver.org/
    if [[ ${1:-} =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9\.]+)*(\+[A-Za-z0-9\.\-]+)?$ ]]; then return 0; else return 1; fi
}

is_release_version() {
    # Test whether the first argument starts with either 'release-' or 'release/' followed by a valid semantic version
    if [[ ${1:-} =~ ^release[/\-].*$ ]]; then is_semantic_version `echo ${1} | sed 's/^release[/-]//'`; else return 1; fi
}

create_release_branch() {
    release_version="${1:-}"
    if ! is_semantic_version ${release_version}; then
        echo "Not a valid version to be released: '${release_version}'!"
        exit 1
    fi
    echo "Removing tag 'release-${release_version}' from ${TRAVIS_BRANCH} and pushing new 'release/${release_version}' branch..."
    git push --delete origin release-${release_version}
    git checkout -b release/${release_version}
    git push origin release/${release_version}
}

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
elif is_release_version ${TRAVIS_BRANCH}; then
    echo "Releasing from branch ${TRAVIS_BRANCH}."
elif [[ ! "${TRAVIS_BRANCH}" =~ ^develop|master$ ]]; then
    echo "Not releasing from branch '${TRAVIS_BRANCH}'."
elif ! is_release_version ${TRAVIS_TAG}; then
    echo "No 'release-x.y.z' tag found, skipping release."
else
    create_release_branch `echo ${TRAVIS_TAG} | sed 's/^release-//'`
fi
