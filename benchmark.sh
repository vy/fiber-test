#!/bin/bash

set -e

projDir=$(dirname $(readlink -f "$0"))

findCpuList() {
    local cpuCount=$( \
        cat "/proc/cpuinfo" | \
        grep "cpu cores" | \
        head -n 1 | \
        sed -r 's/.*([0-9]+)$/\1/g')
    echo "0-$[$cpuCount-1]"
}

[ -z "$workerCount" ] && workerCount=503
[ -z "$ringSize" ] && ringSize=1000000
[ -z "$cpuList" ] && cpuList=$(findCpuList)

if [ "$1" = "-h" -o "$1" = "--help" ]; then
    echo "Available parameters (with defaults):"
    echo "    workerCount ($workerCount)"
    echo "    ringSize    ($ringSize)"
    echo "    cpuList     ($cpuList)"
    exit 0
fi

error() {
    echo "$@" >&2
}

requireFile() {
    local name="$1"
    local path="$2"
    if [ -e "$path" ]
    then echo "$path"
    else
        error "Could not locate $name."
        error "Expected path: $path"
        exit 1
    fi
}

pomFile=$(requireFile "POM file" "$projDir/pom.xml")

artifactId=$( \
    grep "artifactId" "$pomFile" | \
    head -n 1 | \
    sed -r 's/.*<artifactId>(.*)<\/artifactId>/\1/g')
uberJar=$(requireFile \
    "Uber JAR, run \"mvn install\" first" \
    "$projDir/target/$artifactId.jar")
quasarJar="$projDir/target/agents/quasar-core.jar"

cmd="taskset -c $cpuList \
$JAVA_HOME/bin/java \
-server -XX:+TieredCompilation -XX:+AggressiveOpts \
-jar \"$uberJar\" \
-jvmArgsAppend \"-DworkerCount=$workerCount -DringSize=$ringSize -javaagent:$quasarJar\" \
-wi 5 -i 10 -bm avgt -tu ms -f 5 \".*RingBenchmark.*\""
echo "$cmd"
eval "$cmd"
