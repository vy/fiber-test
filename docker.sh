#!/bin/bash

set -euo pipefail
IFS=$'\n\t'

mounted_project_dir="/app"
project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
user_id="$(id -u)"
group_id="$(id -g)"

docker_run() {
    local command="$1"
    docker run \
        --user "$user_id:$group_id" \
        --cpuset-cpus="0" \
        -i -t \
        -v "$project_dir:$mounted_project_dir" \
        -w "$mounted_project_dir" \
        openjdk:11 \
        /bin/bash -c "$command"
}



case "${1:-}" in

    bash)
        docker_run "/bin/bash"
        ;;

    mvn-clean|mvn-package)
        mvn_directive=$(cut -d'-' -f2 <<< "$1")
        docker_run "./mvnw \
            -Dmaven.user.home='$mounted_project_dir/.m2' \
            -Dmaven.repo.local='$mounted_project_dir/.m2/repository' \
            $mvn_directive"
        ;;

    benchmark)
        docker_run "./benchmark/benchmark.py"
        ;;

    *)
        >&2 echo "usage: $0 <bash|mvn-clean|mvn-package|benchmark>"
        exit 1

esac
