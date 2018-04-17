#!/bin/bash

set -euo pipefail
set -x

is_semantic_version() {
    # Test whether the first argument matches a semantic version, see https://semver.org/
    if [[ ${1:-} =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9\.]+)*(\+[A-Za-z0-9\.\-]+)?$ ]]; then return 0; else return 1; fi
}

validate_version() {
    local version="${1:-}"
    if ! is_semantic_version ${version}; then
        echo "[Release] Not a valid version: '${version}'!"
        exit 1
    fi
}

is_release_version() {
    # Test whether the first argument starts with either 'release-' or 'release/' followed by a valid semantic version
    if [[ ${1:-} =~ ^release[/\-].*$ ]]; then is_semantic_version `echo ${1} | sed 's/^release[/-]//'`; else return 1; fi
}

find_release_tag() {
    local tag=`git tag -l --points-at HEAD | grep '^release-' | sed "s/release-//g"`
    echo ${tag:-}
}

remove_release_tag() {
    local release_version="${1:-}"
    validate_version ${release_version}
    echo "[Release] Removing tag 'release-${release_version}'."
    git tag --delete "release-${release_version}"
    git push --delete origin "release-${release_version}" || return 0
}

get_version() {
    TODO
}

set_version() {
    local project_version="${1:-}"
    validate_version ${project_version}
    echo "[Release] Setting project version to '${project_version}'."
    ./mvnw --batch-mode versions:set versions:commit -DnewVersion=${project_version}
    git commit -m "Release: Set version to ${project_version}"
}

create_release_branch() {
    # Delete the 'release-x.y.z' tag and create a new 'release/x.y.z' branch, push it to 'origin'
    local release_version="${1:-}"
    validate_version ${release_version}
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

#----------------------
# MAIN
#----------------------

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
    echo "[Release] Not releasing from pull-request ${TRAVIS_PULL_REQUEST}."
elif is_release_version ${TRAVIS_BRANCH}; then
    echo "[Release] Releasing from branch ${TRAVIS_BRANCH}."
    release_version=`echo ${TRAVIS_BRANCH} | sed 's/release[/]//'`
    validate_version ${release_version}
    set_version ${release_version}
    publish_artifacts
    merge_to_master
elif [[ ! "${TRAVIS_BRANCH}" =~ ^develop|master$ ]]; then
    echo "[Release] Not releasing from branch '${TRAVIS_BRANCH}'."
elif is_release_version ${TRAVIS_TAG}; then
    echo "[Release] Creating new release from tag ${TRAVIS_TAG}"
    release_version=`echo ${TRAVIS_TAG} | sed 's/^release-//'`
    validate_version ${release_version}
    remove_release_tag ${release_version}
    create_release_branch ${release_version}
else
    # TODO Check for SNAPSHOT + deploy
    echo "[Release] No 'release-x.y.z' tag found, skipping release."
fi
