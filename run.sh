#!/bin/sh
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/fario/*.java
exec java -cp out fario.Main
