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

is_pull_request() {
    git ls-remote origin | grep $(git rev-parse HEAD) | grep "refs/pull/"
    return $?
}

find_release_tag() {
    local tag=`git tag -l --points-at HEAD | grep '^release-'`
    echo ${tag:-}
}

find_remote_branches() {
    echo $(git ls-remote --heads origin | grep `git rev-parse HEAD` | sed "s/.*refs\/heads\///g")
}

# env:
#     global:
#         # get all the branches referencing this commit
#         - REAL_BRANCH=$(git ls-remote origin | sed -n "\|$TRAVIS_COMMIT\s\+refs/heads/|{s///p}")
#
#         # or check if we are on a particular branch:
#         - IS_RELEASE=$(git ls-remote origin | grep "$TRAVIS_COMMIT\s\+refs/heads/release$"
# }

remove_release_tag() {
    # Delete the 'release-x.y.z' tag.
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
    # Create a new 'release/x.y.z' branch, push it to 'origin'
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

merge_release_to_master() {
    echo "[Release] TODO: merge $(find_real_branch) back to master"
}

#----------------------
# MAIN
#----------------------

GIT_BRANCHES=$(find_remote_branches)
RELEASE_TAG=$(find_release_tag)

if is_pull_request; then
    echo "[Release] Not releasing from pull-request."
elif is_release_version ${GIT_BRANCHES}; then
    echo "[Release] Releasing from branch ${GIT_BRANCHES}."
    release_version=`echo ${REAL_BRANCH} | sed 's/release[/]//'`
    validate_version ${release_version}
    set_version ${release_version}
    publish_artifacts
    merge_release_to_master
elif [[ ! "${GIT_BRANCHES}" =~ ^develop|master$ ]]; then
    echo "[Release] Not releasing from branch '${GIT_BRANCHES}'."
elif is_release_version ${RELEASE_TAG}; then
    echo "[Release] Creating new release from tag ${RELEASE_TAG}"
    release_version=`echo ${RELEASE_TAG} | sed 's/^release-//'`
    validate_version ${release_version}
    remove_release_tag ${release_version}
    create_release_branch ${release_version}
else
    # TODO Check for SNAPSHOT + deploy
    echo "[Release] No 'release-x.y.z' tag found, skipping release."
fi
