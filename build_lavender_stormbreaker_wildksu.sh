#!/usr/bin/env bash
# Copyright (C) 2021-2022 Oktapra Amtono <oktapra.amtono@gmail.com>
# Refactored for Lavender Stormbreaker WildKSU build by Antigravity AI
# Kernel Build Script for Xiaomi Redmi Note 7 (lavender) - Stormbreaker WildKSU

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Directory paths
KERNEL_DIR="/home/dev/KernelBuild/stormbreaker_official"
CLANG_DIR="/home/dev/KernelBuild/xRageTC-clang"
AK3_DIR="/home/dev/KernelBuild/AnyKernel3-Stormbreaker"
SCRIPT_DIR="/home/dev/KernelBuild/ScriptBuild"
OUT_DIR="$KERNEL_DIR/out"
OUTPUT_DIR="$SCRIPT_DIR/output"
STAGING_DIR="$SCRIPT_DIR/staging"

# Print Header
echo -e "${CYAN}=================================================${NC}"
echo -e "${MAGENTA}   KuyStore Kernel Compiler for Stormbreaker (WildKSU)   ${NC}"
echo -e "${CYAN}=================================================${NC}"

# Variables
KERNEL_NAME="WildSU"
KERNEL_VERSION="x1.0"
CLEAN_OPTION="noclean"
SKIP_BUILD=false
VARIANT="EAS"

# Telegram Settings (Optional)
ENABLE_TELEGRAM=true
TG_BOT_TOKEN="8969845430:AAFSC5uOY43wQYD_n2DgjE4HDYBKCCVwdpY"
TG_CHAT_ID="-1004470799223"

# Parse command line arguments
while [ -n "$1" ]; do
    case "$1" in
        clean|noclean)
            CLEAN_OPTION="$1"
            ;;
        --zip)
            SKIP_BUILD=true
            ;;
        hmp|HMP)
            VARIANT="HMP"
            ;;
        eas|EAS)
            VARIANT="EAS"
            ;;
        *)
            echo -e "${RED}Error: Unknown argument '$1'.${NC}"
            exit 1
            ;;
    esac
    shift
done

# Kernel Local Version Customization
CUSTOM_LOCALVERSION="-WildSU"

# Start counting build duration
BUILD_START=$(date +"%s")

# Local version setup
export LOCALVERSION="-$KERNEL_VERSION"

# Check directories existence
if [ ! -d "$KERNEL_DIR" ]; then
    echo -e "${RED}Error: Kernel source directory '$KERNEL_DIR' does not exist.${NC}"
    exit 1
fi

if [ ! -d "$CLANG_DIR" ]; then
    echo -e "${RED}Error: Clang compiler directory '$CLANG_DIR' does not exist.${NC}"
    exit 1
fi

if [ ! -d "$AK3_DIR" ]; then
    echo -e "${RED}Error: AnyKernel3 template directory '$AK3_DIR' does not exist.${NC}"
    exit 1
fi

# Compile Kernel or Skip
if [ "$SKIP_BUILD" = "true" ]; then
    echo -e "${YELLOW}⏩ Skipping build compilation. Going straight to packaging...${NC}"
else
    # Perform clean build if selected
    if [ "$CLEAN_OPTION" = "clean" ]; then
        echo -e "${YELLOW}Cleaning output directory '$OUT_DIR'...${NC}"
        rm -rf "$OUT_DIR"
    fi

    # Set up toolchain paths & environment
    export PATH="$CLANG_DIR/bin:$PATH"
    CLGV="$("$CLANG_DIR"/bin/clang --version | head -n 1)"
    if [ -f "$CLANG_DIR/bin/ld" ]; then
        BINV="$("$CLANG_DIR"/bin/ld --version | head -n 1)"
    else
        BINV="ld.system"
    fi
    if [ -f "$CLANG_DIR/bin/ld.lld" ]; then
        LLDV="$("$CLANG_DIR"/bin/ld.lld --version | head -n 1)"
    else
        LLDV="ld.lld.system"
    fi
    export KBUILD_COMPILER_STRING="$CLGV - $BINV - $LLDV"

    export ARCH=arm64
    export SUBARCH=arm64
    export KBUILD_BUILD_USER="KuyangID"
    export KBUILD_BUILD_HOST="Ubuntod"
    export HOSTCFLAGS="-fcommon"
    export HOSTLDFLAGS="-fcommon"

    # Define build arguments array to ensure they override kernel Makefile defaults
    MAKE_ARGS=(
        ARCH=arm64
        SUBARCH=arm64
        CC=clang
        CLANG_TRIPLE=aarch64-linux-gnu
        AR=llvm-ar
        NM=llvm-nm
        OBJCOPY=llvm-objcopy
        OBJDUMP=llvm-objdump
        STRIP=llvm-strip
        CROSS_COMPILE=aarch64-linux-gnu-
        CROSS_COMPILE_ARM32=arm-linux-gnueabi-
        HOSTCC=gcc
        HOSTCXX=g++
        HOSTCFLAGS="-fcommon"
        HOSTLDFLAGS="-fcommon"
    )

    # Navigate to kernel source directory
    cd "$KERNEL_DIR"

    # Configure defconfig only if out/.config does not exist
    DEFCONFIG="lavender-perf_defconfig"
    if [ ! -f "out/.config" ]; then
        echo -e "${BLUE}⚙️ Configuring build... Loading defconfig: $DEFCONFIG...${NC}"
        make O=out "${MAKE_ARGS[@]}" "$DEFCONFIG"
    fi

    # Customize config options
    echo -e "${BLUE}⚙️ Customizing config options...${NC}"
    scripts/config --file out/.config --set-str LOCALVERSION "$CUSTOM_LOCALVERSION"
    scripts/config --file out/.config --disable LOCALVERSION_AUTO
    scripts/config --file out/.config --enable CONFIG_OVERLAY_FS
    scripts/config --file out/.config --enable CONFIG_KSU
    scripts/config --file out/.config --disable CONFIG_KSU_KPROBES_HOOK
    scripts/config --file out/.config --disable CONFIG_KSU_LSM_SECURITY_HOOKS

    # Sync configuration changes
    make O=out "${MAKE_ARGS[@]}" olddefconfig

    # Build the kernel
    echo -e "${BLUE}🔧 Compiling kernel with $(nproc --all) threads...${NC}"
    make -j"$(nproc --all)" O=out "${MAKE_ARGS[@]}"
fi

# Check build outcome
KERNEL_IMG="$OUT_DIR/arch/arm64/boot/Image.gz-dtb"
if [ ! -f "$KERNEL_IMG" ]; then
    echo -e "${RED}❌ Error: Build failed! Compiled image not found at $KERNEL_IMG${NC}"
    if [ "$ENABLE_TELEGRAM" = "true" ] && [ -n "$TG_BOT_TOKEN" ] && [ -n "$TG_CHAT_ID" ]; then
        curl -X POST https://api.telegram.org/bot"${TG_BOT_TOKEN}"/sendMessage \
            -d chat_id="${TG_CHAT_ID}" \
            -d text="<b>Failed building Stormbreaker WildKSU kernel for lavender. Please fix it...!</b>" \
            -d "parse_mode=html"
    fi
    exit 1
fi
echo -e "${GREEN}✅ Kernel compiled successfully!${NC}"

# Packaging AnyKernel3 ZIP
if [ -n "$VARIANT" ]; then
    ZIP_NAME="${KERNEL_NAME}_lavender_stormbreaker${LOCALVERSION}-${VARIANT}.zip"
else
    ZIP_NAME="${KERNEL_NAME}_lavender_stormbreaker${LOCALVERSION}.zip"
fi
echo -e "${BLUE}📦 Packaging AnyKernel3 flashable ZIP: $ZIP_NAME...${NC}"

# Setup staging and output directories
mkdir -p "$OUTPUT_DIR"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"

# Stage AnyKernel3 files
cp -r "$AK3_DIR"/* "$STAGING_DIR"/

# Copy kernel image to staging root (where AnyKernel3 searches for it)
cp "$KERNEL_IMG" "$STAGING_DIR"/Image.gz-dtb

# Create zip file
cd "$STAGING_DIR"
zip -r9 "$OUTPUT_DIR/$ZIP_NAME" ./* -x .git README.md *placeholder

# Clean up staging directory
cd "$KERNEL_DIR"
rm -rf "$STAGING_DIR"

# Calculate and print build duration
BUILD_END=$(date +"%s")
DIFF=$((BUILD_END - BUILD_START))
MINUTES=$((DIFF / 60))
SECONDS=$((DIFF % 60))

echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}🎉 Build Successful!${NC}"
echo -e "${GREEN}ZIP file: $OUTPUT_DIR/$ZIP_NAME${NC}"
echo -e "${GREEN}Build duration: $MINUTES minute(s), $SECONDS second(s)${NC}"
echo -e "${GREEN}=================================================${NC}"

# Copy to Windows Documents/KernelZip
WINDOWS_DEST="/mnt/c/Users/KUYANG/Documents/KernelZip"
echo -e "${BLUE}📋 Copying flashable ZIP to Windows Documents/KernelZip...${NC}"
mkdir -p "$WINDOWS_DEST"
cp "$OUTPUT_DIR/$ZIP_NAME" "$WINDOWS_DEST/"
echo -e "${GREEN}✅ Copied to Windows: $WINDOWS_DEST/$ZIP_NAME${NC}"

# Telegram Notification integration
if [ "$ENABLE_TELEGRAM" = "true" ] && [ -n "$TG_BOT_TOKEN" ] && [ -n "$TG_CHAT_ID" ]; then
    echo -e "${BLUE}Sending zip and notification to Telegram...${NC}"
    MD5_SUM=$(md5sum "$OUTPUT_DIR/$ZIP_NAME" | cut -d' ' -f1)
    CAPTION="<b>device :</b> <code>lavender (Stormbreaker WildKSU)</code>
<b>kernel version :</b> <code>$KERNEL_VERSION</code>
<b>md5 checksum :</b> <code>$MD5_SUM</code>
<b>build time :</b> <code>$MINUTES minute, $SECONDS second</code>"

    curl -X POST https://api.telegram.org/bot"${TG_BOT_TOKEN}"/sendDocument \
        -F chat_id="${TG_CHAT_ID}" \
        -F "document=@$OUTPUT_DIR/$ZIP_NAME" \
        --form-string caption="$CAPTION" \
        -F "parse_mode=html" \
        -F "disable_web_page_preview=true"
    
    echo -e "${GREEN}Telegram notification sent!${NC}"
fi
