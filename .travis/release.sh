#!/bin/bash

set -eu -o pipefail

if [[ "${DEBUG:-false}" =~ ^yes|true$ ]]; then set -x; fi

log() {
    message=${1:-}
    echo "$message" 1>&2
}

fatal() {
    log "$1"
    exit 1
}

#
# Versions
#

is_semantic_version() {
    # Test whether the first argument matches a semantic version, see https://semver.org/
    [[ ${1:-} =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9\.]+)*(\+[A-Za-z0-9\.\-]+)?$ ]]
}

validate_version() {
    local version="${1:-}"
    if ! is_semantic_version "${version}"; then fatal "[Release] Not a valid version: '${version}'!"; fi
}

is_snapshot_version() {
    local version="${1:-}"
    validate_version "${version}"
    [[ ${version} =~ ^.*-SNAPSHOT$ ]]
}

is_release_version() {
    # Test whether the first argument starts with either 'release-' or 'release/' followed by a valid semantic version
    if [[ ${1:-} =~ ^release[/\-].*$ ]]; then
        local version=$(echo ${1} | sed 's/^release[/-]//')
        is_semantic_version "${version}" && ! is_snapshot_version "${version}";
    else return 1;
    fi
}

#
# Git
#

is_pull_request() {
    git ls-remote origin | grep $(git rev-parse HEAD) | grep "refs/pull/"
    return $?
}

find_release_tag() {
    echo $(git tag -l --points-at HEAD | grep '^release-')
}

delete_release_tag() {
    # Delete the 'release-x.y.z' tag.
    local release_version="${1:-}"
    validate_version ${release_version}
    log "[Release] Deleting tag 'release-${release_version}'."
    git tag --delete "release-${release_version}"
    git push --delete origin "release-${release_version}" || return 0
}

create_release_branch() {
    # Create a new 'release/x.y.z' branch, push it to 'origin'
    local release_version="${1:-}"
    log "[Release] Pushing new 'release/${release_version}' branch."
    git checkout -b release/${release_version}
    git push origin release/${release_version}
}

get_local_branch() {
    echo "$(git branch | grep '*' | sed 's/[* ]*//')"
}

find_remote_branches() {
    local remote_branches=$(git ls-remote --heads origin | grep `git rev-parse HEAD` | sed "s/.*refs\/heads\///g")
    echo ${remote_branches}
}

merge_release_to_master() {
    log "[Release] TODO: merge $(find_real_branch) back to master"
}

#
# Maven
#

mvn_command() {
    # Does a sanity-check for being in a maven project and return either './mvnw' or 'mvn'.
    if [ ! -f pom.xml ]; then fatal "No maven POM file found!";
    elif [ -x ./mvnw ]; then echo "./mvnw";
    else echo "mvn";
    fi
}

get_maven_version() {
    echo $(printf 'VERSION=${project.version}\n0\n' | $(mvn_command) help:evaluate | grep '^VERSION=' | sed 's/VERSION=//')
}

set_maven_version() {
    log "[Release] Setting project version to '${1}'."
    $(mvn_command) --batch-mode versions:set versions:commit -DnewVersion="${1}"
    git commit -m "Release: Set version to ${1}"
}

publish_maven_artifacts() {
    log "[Release] Publishing released artifacts to maven central."
    ./mvnw --batch-mode -Prelease -nsu -DskipTests deploy
}

#
# Delegation
#

get_version() {
    if [ -f pom.xml ]; then get_maven_version;
    else fatal "[Release] ERROR: No known project structure to determine version of.";
    fi
}

set_version() {
    local project_version="${1:-}"
    validate_version ${project_version}
    if [ -f pom.xml ]; then set_maven_version "${project_version}";
    else fatal "[Release] ERROR: No known project structure to set version for.";
    fi
}

publish_artifacts() {
    if [ -f pom.xml ]; then publish_maven_artifacts;
    else fatal "[Release] ERROR: No known project structure to publish artifacts for.";
    fi
}

#----------------------
# MAIN
#----------------------

if ! is_snapshot_version "1.0.0-SNAPSHOT"; then fatal "[Release] ERROR Snapshot sanity check failed"; fi
if ! is_release_version "release-1.0.0-alpha.1"; then fatal "[Release] ERROR Release sanity check failed"; fi
if is_release_version "release-1.0.0-SNAPSHOT"; then fatal "[Release] ERROR Release sanity check failed"; fi

VERSION=$(get_version)
GIT_BRANCHES=$(find_remote_branches)
RELEASE_TAG=$(find_release_tag)

if is_pull_request; then
    log "[Release] Not releasing from pull-request."
elif is_release_version ${GIT_BRANCHES}; then
    log "[Release] Releasing from branch ${GIT_BRANCHES}."
    release_version=$(echo ${REAL_BRANCH} | sed 's/release[/]//')
    validate_version "${release_version}"
    set_version "${release_version}"
    publish_artifacts
    merge_release_to_master
elif [[ ! "${GIT_BRANCHES}" =~ ^develop|master$ ]]; then
    log "[Release] Not releasing from branch '${GIT_BRANCHES}'."
elif is_release_version ${RELEASE_TAG}; then
    log "[Release] Creating new release from tag ${RELEASE_TAG}"
    release_version=`echo ${RELEASE_TAG} | sed 's/^release-//'`
    validate_version ${release_version}
    delete_release_tag ${release_version}
    create_release_branch ${release_version}
else
    # TODO Check for SNAPSHOT + deploy
    log "[Release] No 'release-x.y.z' tag found, skipping release."
fi
