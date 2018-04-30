#!/bin/bash
set -eu -o pipefail
[[ "${DEBUG:-false}" =~ ^yes|true$ ]] && set -x

declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"
declare -f is_semantic_version > /dev/null || source "$(dirname $0)/versioning.sh"
declare -f is_pull_request > /dev/null || source "$(dirname $0)/git-functions.sh"

create_release() {
    local branch="${1:-}"
    debug "Performing release from branch ${branch}."
    is_release_version "${branch}" || fatal "Not a valid release branch: '${branch}'."
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
    git ls-remote origin
    git remote -v
    git fetch origin master
    git fetch -v origin
    git branch --list --all
#    switch_to_branch master || create_branch master
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

#----------------------
# MAIN
#----------------------

[ -n "${GIT_BRANCH:-}" ] || GIT_BRANCH=$(find_remote_branch)

if is_release_version "${GIT_BRANCH}"; then
    log "Creating a new release from branch '${GIT_BRANCH}'."
    create_release "${GIT_BRANCH}"
else
    log "Nothing to release, not on a release branch: '${GIT_BRANCH}'."
fi