#!/bin/bash

set -eu -o pipefail

if [[ "${DEBUG:-false}" =~ ^yes|true$ ]]; then set -x; fi

#
# Logging
#

SCRIPTNAME=$(basename ${0%.*})
debug() {
    local message="[${SCRIPTNAME}] ${1:-}"
}

log() {
    local message="[${SCRIPTNAME}] ${1:-}"
    echo "$message" 1>&2
}

warn() {
    log "WARNING - ${1:-}"
}

fatal() {
    log "ERROR - ${1:-}"
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
    if ! is_semantic_version "${version}"; then fatal "Not a valid version: '${version}'!"; fi
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

next_snapshot_version() {
    local original="${1:-}"
    validate_version "${original}"
    read base minor suffix <<<$(echo ${original} | perl -pe 's/^(.*?)([0-9]+)([^0-9]*)$/\1 \2 \3/')
    if ! is_snapshot_version ${original}; then suffix=${suffix}-SNAPSHOT; fi
    local nextSnapshot="${base}$((minor+1))${suffix}"
    debug "Next development version from ${original} is ${nextSnapshot}"
    echo ${nextSnapshot}
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
    log "Deleting tag 'release-${release_version}'."
    git tag --delete "release-${release_version}"
    git push --delete origin "release-${release_version}" || return 0
}

create_release_branch() {
    # Create a new 'release/x.y.z' branch, push it to 'origin'
    local release_version="${1:-}"
    log "Pushing new 'release/${release_version}' branch."
    git checkout -b release/${release_version}
    git push origin release/${release_version}
}

get_local_branch() {
    echo "$(git branch | grep '*' | sed 's/[* ]*//')"
}

find_remote_branches() {
    echo $(git ls-remote --heads origin | grep `git rev-parse HEAD` | sed "s/.*refs\/heads\///g")
}

find_remote_branch() {
    local local_branch="$(get_local_branch)"
    local remote_branches=$(find_remote_branches)
    if [[ -n "${local_branch:-}" && "${remote_branches}" = *"${local_branch}" ]]; then echo ${local_branch};
    elif [[ -n "${TRAVIS_BRANCH:-}" && "${remote_branches}" = *"${TRAVIS_BRANCH}" ]]; then echo ${TRAVIS_BRANCH};
    else echo ${remote_branches} | awk '{print $1}';
    fi
}

switch_to_branch() {
    log "Switching to branch ${1}"
    git checkout "${1}"
    git pull
}

create_branch() {
    log "Creating and switching to branch ${1}"
    git checkout -b "${1}"
}

validate_merged_with_remote_branch() {
    if ! git branch -a --merged | grep "remotes/.*/${1}" > /dev/null; then
        fatal "FATAL - Git is not up-to-date with remote branch ${1}, please merge first before proceeding."
    fi
}

#
# Maven
#

mvn_command() {
    if [ ! -f pom.xml ]; then fatal "No maven POM file found!";
    elif [ -x ./mvnw ]; then echo "./mvnw";
    else echo "mvn";
    fi
}

get_maven_version() {
    echo $(printf 'VERSION=${project.version}\n0\n' | $(mvn_command) help:evaluate | grep '^VERSION=' | sed 's/VERSION=//')
}

set_maven_version() {
    $(mvn_command) --batch-mode versions:set versions:commit -DnewVersion="${1}"
}

build_and_test_maven() {
    log "Building and Testing project."
    $(mvn_command) --batch-mode clean verify -Dmaven.test.failure.ignore=false
}

build_and_publish_maven_artifacts() {
    log "Building and Testing project."
    $(mvn_command) --batch-mode clean verify -Dmaven.test.failure.ignore=false -Dmaven.javadoc.skip=true -Dmaven.source.skip=true
    log "Publishing project artifacts to maven central."
    $(mvn_command) --batch-mode --no-snapshot-updates -Prelease deploy -DskipTests
}

#
# Gradle
#

gradle_command() {
    if [ ! -f build.gradle ]; then fatal "No gradle build file found!";
    elif [ -x ./gradlew ]; then echo "./gradlew";
    else echo "gradle";
    fi
}

get_gradle_version() {
    echo $($(gradle_command) properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')
}

set_gradle_version() {
    fatal "TODO Set project version using Gradle"
}

build_and_test_gradle() {
    fatal "TODO Build and test project using Gradle"
}

build_and_publish_gradle_artifacts() {
    fatal "TODO Publish project artifacts using Gradle"
}

#
# NPM
#

get_npm_version() {
    echo $(npm version | head -1 | sed 's/.*'"'"'\(.*\)'"'"'.*/\1/g')
}

set_npm_version() {
    npm version --no-git-tag-version "${1}"
}

build_and_test_npm() {
    fatal "TODO Build and test project using NPM"
}

build_and_publish_npm_artifacts() {
    fatal "TODO Publish project artifacts using NPM"
}

#
# Delegation
#

get_version() {
    if [ -f pom.xml ]; then get_maven_version;
    elif [ -f build.gradle ]; then get_gradle_version;
    elif [ -f package.json ]; then get_npm_version;
    else fatal "ERROR: No known project structure to determine version of.";
    fi
}

set_version() {
    local project_version="${1:-}"
    validate_version ${project_version}
    log "Setting project version to '${project_version}'."

    if [ -f pom.xml ]; then set_maven_version "${project_version}";
    elif [ -f build.gradle ]; then set_gradle_version "${project_version}";
    elif [ -f package.json ]; then set_npm_version "${project_version}"
    else fatal "ERROR: No known project structure to set version for.";
    fi
}

build_and_test() {
    if [ -f pom.xml ]; then build_and_test_maven;
    elif [ -f build.gradle ]; then build_and_test_gradle;
    elif [ -f package.json ]; then build_and_test_npm;
    else fatal "ERROR: No known project structure to publish artifacts for.";
    fi
}

build_and_publish_artifacts() {
    if [ -f pom.xml ]; then build_and_publish_maven_artifacts;
    elif [ -f build.gradle ]; then build_and_publish_gradle_artifacts;
    elif [ -f package.json ]; then build_and_publish_npm_artifacts;
    else fatal "ERROR: No known project structure to publish artifacts for.";
    fi
}

#
# Release proces
#

perform_release() {
    local version=Unknown
    local branch="${1:-}"
    debug "Performing release from branch $branch."
    if is_release_version $branch; then
        version=${branch#*/}
        validate_version "${version}"
        switch_to_branch $branch
    else
        version=${RELEASE_TAG#*-}
        validate_version "${version}"
        if is_snapshot_version "${version}"; then fatal "ERROR Bad release version: ${version}"; fi
        branch="release/${version}"
        create_branch $branch
    fi

    if [[ $(get_version) != ${version} ]]; then
        set_version "${version}"
        git commit -am "Release: Set project version to ${version}"
    fi

    # build_and_publish_artifacts

    local tagname="v${version}"
    log "Tagging published code with '${tagname}'"
    git tag -m "Publish version ${version}" "${tagname}"

    # Merge to master and delete release branch (local+remote)
    log "Merging ${release_branch} to master"
    switch_to_branch master
    git merge --no-edit --ff-only "${release_branch}"
    git branch -d "${release_branch}"

    # Merge to develop and switch to next snapshot version
    local nextSnapshot=$(next_snapshot_version ${version})
    log "Merging to develop and updating version to ${nextSnapshot}"
    switch_to_branch develop
    git merge --no-edit master
    set_version ${nextSnapshot}
    git commit -am "Release: Set version to ${nextSnapshot}"

    # Pushing local changes to remote
#    git push origin "${tagname}"
#    git push origin master
#    git push origin develop
#    git push origin --delete "${release_branch}"
}

#----------------------
# MAIN
#----------------------

[ -n "${VERSION:-}" ] || VERSION=$(get_version)
[ -n "${GIT_BRANCH:-}" ] || GIT_BRANCH=$(find_remote_branch)
[ -n "${RELEASE_TAG:-}" ] || RELEASE_TAG=$(find_release_tag)

if is_pull_request; then
    log "Testing code for pull-request."
    build_and_test
elif is_release_version ${GIT_BRANCH} || is_release_version ${RELEASE_TAG}; then
    log "Releasing from branch ${GIT_BRANCH}."
    perform_release ${GIT_BRANCH}
elif [[ ! "${GIT_BRANCH}" = "develop" && ! "${GIT_BRANCH}" = "master" ]]; then
    log "Not publishing from branch '${GIT_BRANCH}', running a test build."
    build_and_test
elif is_snapshot_version "${VERSION}"; then
    log "Deploying snapshot from branch '${GIT_BRANCH}'."
    build_and_publish_artifacts
else
    log "Not publishing artifacts; no snapshot version found on branch '${GIT_BRANCH}'."
    build_and_test
fi
