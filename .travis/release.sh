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

set_version() {
    project_version="${1:-}"
    if ! is_semantic_version ${project_version}; then
        echo "[Release] Not a valid project version: '${project_version}'!"
        exit 1
    fi
    echo "[Release] Setting project version to '${project_version}'."
    ./mvnw --batch-mode versions:set versions:commit -DnewVersion=${project_version}
    git commit -m "Release: Set version to ${project_version}"
}

create_release_branch() {
    # Delete the 'release-x.y.z' tag and create a new 'release/x.y.z' branch, push it to 'origin'
    release_version="${1:-}"
    if ! is_semantic_version ${release_version}; then
        echo "[Release] Not a valid version to be released: '${release_version}'!"
        exit 1
    fi
    echo "[Release] Removing tag 'release-${release_version}'."
    git push --delete origin release-${release_version}
    echo "[Release] Pushing new 'release/${release_version}' branch."
    git checkout -b release/${release_version}
    git push origin release/${release_version}
}

publish_artifacts() {
    echo "[Release] Publishing released artifacts to maven central."
    ./mvnw --batch-mode -Prelease -nsu -DskipTests deploy
}

merge_to_master() {
    echo "[Release] TODO: merge ${TRAVIS_BRANCH} back to master"
}

perform_release() {
    release_version="${1:-}"
    if ! is_semantic_version ${release_version}; then
        echo "[Release] Not a valid version to be released: '${release_version}'!"
        exit 1
    fi
    set_version ${release_version}
    publish_artifacts
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
    echo "[Release] Not releasing from pull-request ${TRAVIS_PULL_REQUEST}."
elif is_release_version ${TRAVIS_BRANCH}; then
    perform_release `echo ${TRAVIS_BRANCH} | sed 's/release[/]//'`
elif [[ ! "${TRAVIS_BRANCH}" =~ ^develop|master$ ]]; then
    echo "[Release] Not releasing from branch '${TRAVIS_BRANCH}'."
elif is_release_version ${TRAVIS_TAG}; then
    create_release_branch `echo ${TRAVIS_TAG} | sed 's/^release-//'`
else
    echo "[Release] No 'release-x.y.z' tag found, skipping release."
fi
