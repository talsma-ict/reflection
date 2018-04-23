#!/bin/bash
set -eu -o pipefail

declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"

#
# Script containing pre-defined functions regarding versioning.
#

# Test whether the first argument matches a semantic version, see https://semver.org/
# Although this function is not 100% strict, the used regular expression is a decent approximation of the semver 2.0
# standard.
#
# params:   1: The version to test
# result:   whether the first argument was a valid semantic version.

is_semantic_version() {
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
