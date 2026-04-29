#!/bin/bash

DIR=$(dirname $0)
pushd $DIR

DEPVER=17
SQLCIPHER_VER=4.5.4

download_file() {
  local url="$1"
  local output="$2"

  if which wget
  then
    wget -c "$url" -O "$output"
  else
    curl -L "$url" -o "$output"
  fi
}

apply_freerdp_patch() {
  local patch_file="$1"
  local freerdp_dir="remoteClientLib/jni/libs/deps/FreeRDP"

  if [ ! -d "$freerdp_dir" ]
  then
    echo "FreeRDP dependency directory not found; skipping $patch_file"
    return
  fi

  if patch -d "$freerdp_dir" -p1 --forward --dry-run < "$patch_file" >/dev/null 2>&1
  then
    patch -d "$freerdp_dir" -p1 --forward < "$patch_file"
  else
    echo "$patch_file already applied or not applicable; skipping."
  fi
}

download_file \
  "https://github.com/iiordanov/remote-desktop-clients/releases/download/dependencies/remote-desktop-clients-libs-${DEPVER}.tar.gz" \
  "remote-desktop-clients-libs-${DEPVER}.tar.gz"

rm -rf remoteClientLib/jni/libs/deps/FreeRDP/
mkdir -p remoteClientLib/jni/libs/deps/FreeRDP/

tar xf remote-desktop-clients-libs-${DEPVER}.tar.gz

apply_freerdp_patch remoteClientLib/jni/libs/19_freerdp_add_glyph_cache_to_bookmark.patch
apply_freerdp_patch remoteClientLib/jni/libs/20_freerdp_toggle_glyph_cache_in_libfreerdp.patch

if [ ! -d remoteClientLib/src/main/jniLibs -a -d remoteClientLib/libs/ ]
then
  rm -f remoteClientLib/src/main/jniLibs
  cp -a remoteClientLib/libs/ remoteClientLib/src/main/jniLibs/
fi

if [ ! -f "common/aars/android-database-sqlcipher-${SQLCIPHER_VER}.aar" ]
then
  mkdir -p common/aars
  download_file \
    "https://repo1.maven.org/maven2/net/zetetic/android-database-sqlcipher/${SQLCIPHER_VER}/android-database-sqlcipher-${SQLCIPHER_VER}.aar" \
    "common/aars/android-database-sqlcipher-${SQLCIPHER_VER}.aar"
else
  echo "SQL Cipher ${SQLCIPHER_VER} aar found locally"
fi

echo "Done downloading and extracting dependencies."
popd
