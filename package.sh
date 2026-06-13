#!/bin/sh
# Build a native installer with jpackage.
#
#   ./package.sh <type> [version]
#
# <type> is a jpackage installer type valid for the current OS, e.g.
#   macOS:   dmg, pkg
#   Windows: exe, msi   (requires the WiX Toolset on PATH)
#   Linux:   deb, rpm
#
# If [version] is omitted the VERSION file is used. jpackage bundles its own
# Java runtime, so the resulting installer needs no Java on the target machine.
set -e
cd "$(dirname "$0")"

TYPE="${1:?usage: ./package.sh <type> [version]}"
VERSION="${2:-$(tr -d ' \t\r\n' < VERSION)}"

# Always build a fresh jar first.
./build.sh

rm -rf installer
mkdir -p installer

# Platform specific options.
EXTRA=""
case "$(uname -s)" in
  Darwin)
    EXTRA="--mac-package-identifier com.github.rickhw.fario"
    ;;
  Linux)
    EXTRA="--linux-shortcut --linux-menu-group Games --linux-deb-maintainer rick.kyhwang@gmail.com"
    ;;
  MINGW*|MSYS*|CYGWIN*)
    EXTRA="--win-shortcut --win-menu --win-menu-group Games --win-dir-chooser"
    ;;
esac

echo "Packaging Fario $VERSION as '$TYPE' ..."
# shellcheck disable=SC2086
jpackage \
  --type "$TYPE" \
  --name Fario \
  --app-version "$VERSION" \
  --input dist \
  --main-jar fario.jar \
  --main-class mario.Main \
  --dest installer \
  --vendor "Rick Hwang" \
  --copyright "Copyright (c) 2026 Rick Hwang" \
  --description "A Super Mario style side-scrolling platformer written in pure Java" \
  --license-file LICENSE \
  --about-url "https://github.com/rickhw/fario" \
  $EXTRA

echo "OK -> installer/"
ls -1 installer
