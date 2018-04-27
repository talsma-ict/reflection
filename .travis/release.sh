#!/bin/bash

declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"
declare -f is_semantic_version > /dev/null || source "$(dirname $0)/versioning.sh"
declare -f is_pull_request > /dev/null || source "$(dirname $0)/git-functions.sh"
declare -f is_maven_project > /dev/null || source "$(dirname $0)/maven-functions.sh"
#declare -f is_gradle_project > /dev/null || source "$(dirname $0)/gradle-functions.sh"
#declare -f is_npm_project > /dev/null || source "$(dirname $0)/npm-functions.sh"

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

create_release() {
    local branch="${1:-}"
    debug "Performing release from branch ${branch}."
    is_release_version "${branch}" || fatal "Branch is not a valid release branch: '${branch}'."
    local release_version="${branch#*/}"
    debug "Detected version '${release_version}'."
    validate_version "${release_version}"
    switch_to_branch "${branch}" || create_branch "${branch}"
    log "Releasing version ${release_version} from branch ${branch}."

    local current_version="$(get_version)"
    if [[ "${current_version}" != "${release_version}" ]]; then
        log "Updating version from ${current_version} to ${release_version}."
        set_version "${release_version}"
        git commit -s -am "Release: Set project version to ${release_version}"
    else
        log "No need to update project version. It is already '${release_version}'."
    fi

    local tagname="${release_version}"
    log "Tagging published code with '${tagname}'."
    git tag -m "Publish version ${release_version}" "${tagname}"

    # Merge to master and delete local release branch
    log "Merging ${branch} to master"
    switch_to_branch master || create_branch master
    [[ "$(get_local_branch)" = "master" ]] || fatal "Could not switch to master branch."
    git merge --no-edit --ff-only "${branch}"
    git branch -d "${branch}" || warn "Could not delete local release branch '${branch}'."

    # Merge to develop and switch to next snapshot
    local nextSnapshot="$(next_snapshot_version ${release_version})"
    log "Merging to develop and updating version to '${nextSnapshot}'."
    switch_to_branch develop || create_branch develop
    [[ "$(get_local_branch)" = "develop" ]] || fatal "Could not switch to develop branch."
    git merge --no-edit master
    set_version ${nextSnapshot}
    git commit -s -am "Release: Set next development version to ${nextSnapshot}"

    log "Pushing release to origin and deleting branch '${branch}'."
    git push origin "${tagname}"
    git push origin master
    git push origin develop
    git push origin --delete "${branch}"
}
