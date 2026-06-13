#!/bin/sh
# Compile the sources and produce a runnable jar at dist/fario.jar.
# The version is taken from the VERSION file and embedded in the jar manifest,
# so the running game can display it (see mario.Main).
set -e
cd "$(dirname "$0")"

VERSION="$(tr -d ' \t\r\n' < VERSION)"
echo "Building Fario $VERSION ..."

rm -rf out dist
mkdir -p out dist

javac -d out src/mario/*.java

cat > out/manifest.mf <<EOF
Main-Class: mario.Main
Implementation-Title: Fario
Implementation-Version: $VERSION
EOF

jar --create --file dist/fario.jar --manifest out/manifest.mf -C out mario

echo "OK -> dist/fario.jar"
echo "Run with: java -jar dist/fario.jar"
