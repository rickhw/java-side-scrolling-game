#!/bin/sh
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/mario/*.java
exec java -cp out mario.Main
