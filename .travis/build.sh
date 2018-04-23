#!/bin/bash
set -eu -o pipefail
if [[ "${DEBUG:-false}" =~ ^yes|true$ ]]; then set -x; fi

# Import functions if not already imported
declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"
declare -f is_semantic_version > /dev/null || source "$(dirname $0)/versioning.sh"
declare -f is_pull_request > /dev/null || source "$(dirname $0)/git-functions.sh"

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
    local branch="${1:-}"
    debug "Performing release from branch $branch."
    local version=Unknown
    if is_release_version ${branch}; then
        version=${branch#*/}
        validate_version "${version}"
        switch_to_branch ${branch}
    else
        version=${RELEASE_TAG#*-}
        validate_version "${version}"
        if is_snapshot_version "${version}"; then fatal "ERROR Bad release version: ${version}"; fi
        branch="release/${version}"
        create_branch ${branch}
        git tag --delete "release-${version}"
        git push --delete origin "release-${version}"
    fi
    log "Releasing verion ${version} from branch ${branch}."

    if [[ $(get_version) != ${version} ]]; then
        set_version "${version}"
        git commit -am "Release: Set project version to ${version}"
    fi

    # build_and_publish_artifacts

    local tagname="v${version}"
    log "Tagging published code with '${tagname}'"
    git tag -m "Publish version ${version}" "${tagname}"

    # Merge to master and delete release branch (local+remote)
    log "Merging ${branch} to master"
    switch_to_branch master
    git merge --no-edit --ff-only "${branch}"
    git branch -d "${branch}"

    # Merge to develop and switch to next snapshot version
    local nextSnapshot=$(next_snapshot_version ${version})
    log "Merging to develop and updating version to ${nextSnapshot}"
    switch_to_branch develop
    git merge --no-edit master
    set_version ${nextSnapshot}
    git commit -am "Release: Set version to ${nextSnapshot}"

    # Pushing local changes to remote
    git push origin "${tagname}"
    git push origin master
    git push origin develop
    if is_release_version ${GIT_BRANCH}; then git push origin --delete "${branch}"; fi
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
#    log "Not publishing artifacts; no snapshot version found on branch '${GIT_BRANCH}'."
#    build_and_test
    log "Skipping build for branch '${GIT_BRANCH}'."
fi
