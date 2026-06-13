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

# jpackage requires app-version's first component to be >= 1, but macOS (CFBundle)
# also rejects it. For a 0.x release we hand macOS a bundle version of 1.0.0 and
# rename the produced file back to the real version afterwards. Everything else
# (jar manifest, displayed version, filename) keeps the true VERSION.
APP_VERSION="$VERSION"
ICON=""
EXTRA=""
case "$(uname -s)" in
  Darwin)
    [ "${VERSION%%.*}" = "0" ] && APP_VERSION="1.0.0"
    [ -f assets/icon.icns ] && ICON="assets/icon.icns"
    EXTRA="--mac-package-identifier com.github.rickhw.fario"
    ;;
  Linux)
    [ -f assets/icon.png ] && ICON="assets/icon.png"
    EXTRA="--linux-shortcut --linux-menu-group Games --linux-deb-maintainer rick.kyhwang@gmail.com"
    ;;
  MINGW*|MSYS*|CYGWIN*)
    [ -f assets/icon.ico ] && ICON="assets/icon.ico"
    EXTRA="--win-shortcut --win-menu --win-menu-group Games --win-dir-chooser"
    ;;
esac
[ -n "$ICON" ] && EXTRA="$EXTRA --icon $ICON"

echo "Packaging Fario $VERSION (bundle version $APP_VERSION) as '$TYPE' ..."
# shellcheck disable=SC2086
jpackage \
  --type "$TYPE" \
  --name Fario \
  --app-version "$APP_VERSION" \
  --input dist \
  --main-jar fario.jar \
  --main-class fario.Main \
  --dest installer \
  --vendor "Rick Hwang" \
  --copyright "Copyright (c) 2026 Rick Hwang" \
  --description "An 8-bit style side-scrolling platformer written in pure Java" \
  --license-file LICENSE \
  --about-url "https://github.com/rickhw/fario" \
  $EXTRA

# Rename installer to carry the real version when it differs from the bundle one.
if [ "$APP_VERSION" != "$VERSION" ]; then
  for f in installer/*"$APP_VERSION"*; do
    [ -e "$f" ] || continue
    mv "$f" "$(echo "$f" | sed "s/$APP_VERSION/$VERSION/")"
  done
fi

echo "OK -> installer/"
ls -1 installer
