#!/bin/bash

declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"

#----------------------
# Script containing pre-defined functions regarding versioning.
#----------------------

# Test whether the first argument is a valid 'sementic version', see https://semver.org
is_semantic_version() {
    [[ ${1:-} =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9\.]+)*(\+[A-Za-z0-9\.\-]+)?$ ]]
}

validate_version() {
    is_semantic_version "${1:-}" || fatal "Not a valid version: '${1:-}'!"
}

is_snapshot_version() {
    validate_version "${1:-}"
    [[ "${1:-}" =~ ^.*-SNAPSHOT$ ]]
}

# Test whether the first argument starts with either 'release-' or 'release/' followed by a valid semantic version
is_release_version() {
    if [[ ${1:-} =~ ^release[/\-].*$ ]]; then
        local version=$(echo ${1} | sed 's/^release[/-]//')
        is_semantic_version "${version}" && ! is_snapshot_version "${version}";
    else return 1;
    fi
}

# Returns the next snapshot version for the version specified in the first argument
next_snapshot_version() {
    validate_version "${1:-}"
    read base minor suffix <<<$(echo "${1}" | perl -pe 's/^(.*?)([0-9]+)([^0-9]*)$/\1 \2 \3/')
    if ! is_snapshot_version ${1}; then suffix=${suffix}-SNAPSHOT; fi
    local nextSnapshot="${base}$((minor+1))${suffix}"
    debug "Next development version from ${1} is ${nextSnapshot}"
    echo ${nextSnapshot}
}
