#!/bin/bash
set -eu -o pipefail
if [[ "${DEBUG:-false}" =~ ^yes|true$ ]]; then set -x; fi

#
# Script containing (very) basic logging and debugging features
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
