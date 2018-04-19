#!/bin/bash

set -eu -o pipefail

if [[ "${DEBUG:-false}" =~ ^yes|true$ ]]; then set -x; fi

debug() {
    local message=${1:-}
}

log() {
    local message=${1:-}
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

next_snapshot_version() {
    local original="${1:-}"
    validate_version "${original}"
    read base minor suffix <<<$(echo ${original} | perl -pe 's/^(.*?)([0-9]+)([^0-9]*)$/\1 \2 \3/')
    if ! is_snapshot_version ${original}; then suffix=${suffix}-SNAPSHOT; fi
    local nextSnapshot="${base}$((minor+1))${suffix}"
    debug "[Release] Next development version from ${original} is ${nextSnapshot}"
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
    log "[Release] Switching to branch ${1}"
    git checkout "${1}"
    git pull
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
    git commit -am "Release: Set project version to ${1}"
}

publish_maven_artifacts() {
    log "[Release] Publishing project artifacts to maven central."
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
    fatal "[Release] TODO Set project version using Gradle"
}

publish_gradle_artifacts() {
    fatal "[Release] TODO Publish project artifacts using Gradle"
}

#
# NPM
#

get_npm_version() {
    echo $(npm version | head -1 | sed 's/.*'"'"'\(.*\)'"'"'.*/\1/g')
}

set_npm_version() {
    npm version --no-git-tag-version "${1}"
    git commit -am "Release: Set project version to ${1}"
}

publish_npm_artifacts() {
    fatal "[Release] TODO Publish project artifacts using NPM"
}

#
# Delegation
#

get_version() {
    if [ -f pom.xml ]; then get_maven_version;
    elif [ -f build.gradle ]; then get_gradle_version;
    elif [ -f package.json ]; then get_npm_version;
    else fatal "[Release] ERROR: No known project structure to determine version of.";
    fi
}

set_version() {
    local project_version="${1:-}"
    validate_version ${project_version}
    log "[Release] Setting project version to '${project_version}'."

    if [ -f pom.xml ]; then set_maven_version "${project_version}";
    elif [ -f build.gradle ]; then set_gradle_version "${project_version}";
    elif [ -f package.json ]; then set_npm_version "${project_version}"
    else fatal "[Release] ERROR: No known project structure to set version for.";
    fi
}

publish_artifacts() {
    if [ -f pom.xml ]; then publish_maven_artifacts;
    elif [ -f build.gradle ]; then publish_gradle_artifacts;
    elif [ -f package.json ]; then publish_npm_artifacts;
    else fatal "[Release] ERROR: No known project structure to publish artifacts for.";
    fi
}

#
# Release proces
#

release_and_finish_branch() {
    local release_branch="${1:-}"
    release_version=$(echo ${release_branch} | sed 's/release[/]//')
    validate_version "${release_version}"
    if is_snapshot_version "${release_version}"; then fatal "[Release] ERROR Bad release branch: ${release_branch}"; fi
    if [[ $(get_local_branch) != ${release_branch} ]]; then
        switch_to_branch ${release_branch}
    fi
    if [[ $(get_version) != ${release_version} ]]; then
        set_version "${release_version}"
        git commit --sign -am "Set version to ${release_version}"
    fi

    # Publish release and push a tag
    publish_artifacts
    local tagname="v${release_version}"
    log "[Release] Tagging published code with '${tagname}'"
    git tag --sign -m "Publish version ${release_version}" "${tagname}"
    git push origin "${tagname}"

    # Merge to master and delete release branch (local+remote)
    log "[Release] Merging ${release_branch} to master"
    switch_to_branch master
    git merge --no-edit --ff-only "${release_branch}"
    git push origin master
    validate_merged_with_remote master
    git branch -d "${release_branch}"
    git push origin --delete "${release_branch}"

    # Merge to develop and switch to next snapshot version
    local nextSnapshot=$(next_snapshot_version ${release_version})
    log "[Release] Merging release to develop and updating version to ${nextSnapshot}"
    switch_to_branch develop
    validate_merged_with_remote develop
    git merge --no-edit master
    set_version ${nextSnapshot}
    git commit --sign -am "Set version to ${nextSnapshot}"
    git push origin develop
}

#----------------------
# MAIN
#----------------------

[ -n "${VERSION:-}" ] || VERSION=$(get_version)
[ -n "${GIT_BRANCH:-}" ] || GIT_BRANCH=$(find_remote_branch)
[ -n "${RELEASE_TAG:-}" ] || RELEASE_TAG=$(find_release_tag)

if is_pull_request; then
    log "[Release] Not releasing from pull-request."
elif is_release_version ${GIT_BRANCH}; then
    log "[Release] Releasing from branch ${GIT_BRANCH}."
    release_and_finish_branch "${GIT_BRANCH}"
elif [[ ! "${GIT_BRANCH}" = "develop" && ! "${GIT_BRANCH}" = "master" ]]; then
    log "[Release] Not releasing from branch '${GIT_BRANCH}'."
elif is_release_version ${RELEASE_TAG}; then
    log "[Release] Creating new release from tag ${RELEASE_TAG}"
    release_version=`echo ${RELEASE_TAG} | sed 's/^release-//'`
    validate_version ${release_version}
    delete_release_tag ${release_version}
    create_release_branch ${release_version}
elif is_snapshot_version "${VERSION}"; then
    log "[Release] Deploying snapshot from branch '${GIT_BRANCH}'."
    publish_artifacts
else
    log "[Release] Not publishing artifacts; no 'release-x.y.z' tag nor snapshot version found on branch '${GIT_BRANCH}'."
fi
