#!/bin/bash
set -e

ARCH_X86=x86
ARCH_X64=x86_64
ARCH_ARM=armeabi-v7a
ARCH_ARM64=arm64-v8a

NEEDS_CLEAN='false'
FLAGS=

usage() {
    printf "Usage: %s -a arch [-c]" "$0"
    printf "\t-a Specify an architecture (i.e. '%s', '%s', '%s', '%s')", $ARCH_X86 $ARCH_X64 $ARCH_ARM $ARCH_ARM64
    printf "\t-c Clean previous builds\n"
    exit 1 # Exit script after printing help
}

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    HOST_TAG=linux-x86_64
elif [[ "$OSTYPE" == "darwin"* ]]; then
    HOST_TAG=darwin-x86_64
fi

if [ -z "$HOST_TAG" ]; then
    echo "Unkown host tag for OS: $OSTYPE"
    exit 1
fi

while getopts "a:cd" opt; do
    case "$opt" in
    a) ARCH_NAME="$OPTARG" ;;
    c) NEEDS_CLEAN="true" ;;
    d) FLAGS+="${FLAGS} -DDEBUG" ;;
    \?) usage ;; # Print usage in case parameter is non-existent
    esac
done

if [ -z "$ARCH_NAME" ]; then
    echo "No architecture specified"
    usage
fi

# NDK_PATH must be set
if [[ -z ${NDK_PATH+x} ]]; then
    echo "NDK_PATH is unset, should be somewhere like /Users/<username>/Library/Android/sdk/ndk/22.1.7171670"
    exit 1
fi

# Common variables.
TOOLCHAIN=$NDK_PATH/toolchains/llvm/prebuilt/$HOST_TAG
export LIBRIVE=$PWD/../submodules/rive-cpp
export SYSROOT=$TOOLCHAIN/sysroot
export INCLUDE=$SYSROOT/usr/include
export INCLUDE_CXX=$INCLUDE/c++/v1
export CXXFLAGS="-std=c++17 -Wall -fno-exceptions -fno-rtti -Iinclude -fPIC -Oz ${FLAGS}"
export AR=$TOOLCHAIN/bin/llvm-ar

SKIA_ARCH=

buildFor() {
    # Build skia
    pushd "$LIBRIVE"/skia/dependencies
    ./make_skia_android.sh "$SKIA_ARCH"

    popd

    # Build librive_skia_renderer (internally builds librive)
    pushd "$LIBRIVE"/skia/renderer
    if ${NEEDS_CLEAN}; then
        ./build.sh -p android."$SKIA_ARCH" clean
    fi
    ./build.sh -p android."$SKIA_ARCH" release
    popd

    # Cleanup our android build location.
    mkdir -p "$BUILD_DIR"
    if ${NEEDS_CLEAN}; then
        # echo 'cleaning!'
        make clean
    fi

    # copy in newly built rive/skia/skia_renderer files.
    cp "$LIBRIVE"/build/android/"$SKIA_ARCH"/bin/release/librive.a "$BUILD_DIR"
    cp "$LIBRIVE"/skia/dependencies/skia_rive_optimized/out/"$SKIA_ARCH"/libskia.a "$BUILD_DIR"
    cp "$LIBRIVE"/skia/renderer/build/android/"$SKIA_ARCH"/bin/release/librive_skia_renderer.a "$BUILD_DIR"
    cp "$LIBCXX"/libc++_static.a "$BUILD_DIR"

    # build the android .so!
    mkdir -p "$BUILD_DIR"/obj
    make -j7

    JNI_DEST=../kotlin/src/main/jniLibs/$ARCH_NAME
    mkdir -p "$JNI_DEST"
    cp "$BUILD_DIR"/libjnirivebridge.so "$JNI_DEST"
}

API=21

if [ "$ARCH_NAME" = "$ARCH_X86" ]; then
    echo "==== x86 ===="
    SKIA_ARCH=x86
    ARCH=i686
    export BUILD_DIR=$PWD/build/$ARCH_NAME
    export CC=$TOOLCHAIN/bin/$ARCH-linux-android$API-clang
    export CXX=$TOOLCHAIN/bin/$ARCH-linux-android$API-clang++
    LIBCXX=$SYSROOT/usr/lib/$ARCH-linux-android
elif [ "$ARCH_NAME" = "$ARCH_X64" ]; then
    echo "==== x86_64 ===="
    ARCH=x86_64
    SKIA_ARCH=x64
    export BUILD_DIR=$PWD/build/$ARCH_NAME
    export CXX=$TOOLCHAIN/bin/$ARCH-linux-android$API-clang++
    export CC=$TOOLCHAIN/bin/$ARCH-linux-android$API-clang
    LIBCXX=$SYSROOT/usr/lib/$ARCH-linux-android
elif [ "$ARCH_NAME" = "$ARCH_ARM" ]; then
    echo "==== ARMv7 ===="
    ARCH=arm
    ARCH_PREFIX=armv7a
    SKIA_ARCH=arm
    export BUILD_DIR=$PWD/build/$ARCH_NAME
    export CXX=$TOOLCHAIN/bin/$ARCH_PREFIX-linux-androideabi$API-clang++
    export CC=$TOOLCHAIN/bin/$ARCH_PREFIX-linux-androideabi$API-clang
    LIBCXX=$SYSROOT/usr/lib/$ARCH-linux-androideabi
elif [ "$ARCH_NAME" = "$ARCH_ARM64" ]; then
    echo "==== ARM64 ===="
    ARCH=aarch64
    SKIA_ARCH=arm64
    export BUILD_DIR=$PWD/build/$ARCH_NAME
    export CXX=$TOOLCHAIN/bin/$ARCH-linux-android$API-clang++
    export CC=$TOOLCHAIN/bin/$ARCH-linux-android$API-clang
    LIBCXX=$SYSROOT/usr/lib/$ARCH-linux-android
else
    echo "Invalid architecture specified: '$ARCH_NAME'"
    usage
fi

buildFor
