#!/bin/bash
set -eu -o pipefail
[[ "${DEBUG:-false}" =~ ^yes|true$ ]] && set -x

# Import functions if not already imported
declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"
declare -f is_semantic_version > /dev/null || source "$(dirname $0)/versioning.sh"
declare -f is_pull_request > /dev/null || source "$(dirname $0)/git-functions.sh"
declare -f is_maven_project > /dev/null || source "$(dirname $0)/maven-functions.sh"
#declare -f is_gradle_project > /dev/null || source "$(dirname $0)/gradle-functions.sh"
#declare -f is_npm_project > /dev/null || source "$(dirname $0)/npm-functions.sh"

#
# Delegation
#

get_version() {
    if is_maven_project; then get_maven_version;
#    elif is_gradle_project; then get_gradle_version;
#    elif [ -f package.json ]; then get_npm_version;
    else fatal "ERROR: No known project structure to determine version of.";
    fi
}

set_version() {
    local project_version="${1:-}"
    validate_version ${project_version}
    log "Setting project version to '${project_version}'."

    if is_maven_project; then set_maven_version "${project_version}";
#    elif is_gradle_project; then set_gradle_version "${project_version}";
#    elif [ -f package.json ]; then set_npm_version "${project_version}"
    else fatal "ERROR: No known project structure to set version for.";
    fi
}

build_and_test() {
    if is_maven_project; then build_and_test_maven;
#    elif is_gradle_project; then build_and_test_gradle;
#    elif [ -f package.json ]; then build_and_test_npm;
    else fatal "ERROR: No known project structure to publish artifacts for.";
    fi
}

build_and_publish_artifacts() {
    if is_maven_project; then build_and_publish_maven_artifacts;
#    elif is_gradle_project; then build_and_publish_gradle_artifacts;
#    elif is_npm_project; then build_and_publish_npm_artifacts;
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

    build_and_publish_artifacts

    local tagname="v${version}"
    log "Tagging published code with '${tagname}'"
    git tag -m "Publish version ${version}" "${tagname}"

    # Merge to master and delete release branch (local+remote)
    log "Merging ${branch} to master"
    switch_to_branch master || create_branch master
    git merge --no-edit --ff-only "${branch}"
    git branch -d "${branch}"

    # Merge to develop and switch to next snapshot version
    local nextSnapshot=$(next_snapshot_version ${version})
    log "Merging to develop and updating version to ${nextSnapshot}"
    switch_to_branch develop || create_branch develop
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
RELEASE_TAG=$(find_release_tag)

if is_pull_request; then
    log "Testing code for pull-request."
    build_and_test
elif is_release_version "${GIT_BRANCH}" || is_release_version "${RELEASE_TAG}"; then
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
