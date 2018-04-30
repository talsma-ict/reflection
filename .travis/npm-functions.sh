#!/bin/bash

declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"

#
# NPM
#

is_npm_project() {
    [ -f package.json ]
}

get_npm_version() {
#    is_npm_project || fatal "No package.json file found for NPM build"
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

