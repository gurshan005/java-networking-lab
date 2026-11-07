#!/bin/sh
set -eu
cd "$(dirname "$0")"
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
javac --release 17 -d "$build" src/*.java
java -cp "$build" RoutingDemo
java -cp "$build" LoopbackCheck
