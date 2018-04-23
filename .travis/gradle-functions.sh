#!/bin/bash
set -eu -o pipefail

declare -f debug > /dev/null || source "$(dirname $0)/logging.sh"

#
# Gradle
#

is_gradle_project() {
    [ -f build.gradle ]
}

gradle_command() {
#    is_gradle_project || fatal "No gradle build file found!"
    if [ -x ./gradlew ]; then echo "./gradlew";
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
