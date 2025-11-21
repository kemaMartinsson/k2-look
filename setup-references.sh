#!/bin/bash
# Setup Reference Projects Script
# This script clones the reference repositories if they don't exist

echo -e "\e[36mSetting up reference projects...\e[0m"
echo ""

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REFERENCE_DIR="$SCRIPT_DIR/reference"

# Create reference directory if it doesn't exist
if [ ! -d "$REFERENCE_DIR" ]; then
    echo -e "\e[33mCreating reference directory...\e[0m"
    mkdir -p "$REFERENCE_DIR"
fi

# Function to clone or update a repository
setup_repository() {
    local url=$1
    local name=$2
    local path=$3

    echo -e "\e[33mSetting up $name...\e[0m"

    if [ -d "$path" ]; then
        echo -e "  \e[32m✓ $name already exists at: $path\e[0m"
        echo -e "  \e[90mUse update-references.sh to update it\e[0m"
    else
        echo -e "  \e[90mCloning from: $url\e[0m"
        pushd "$REFERENCE_DIR" > /dev/null
        if git clone "$url"; then
            echo -e "  \e[32m✓ $name cloned successfully\e[0m"
        else
            echo -e "  \e[31m✗ Error cloning $name\e[0m"
            [ -d "$path" ] && rm -rf "$path"
            popd > /dev/null
            exit 1
        fi
        popd > /dev/null
    fi
    echo ""
}

echo -e "\e[90mReference directory: $REFERENCE_DIR\e[0m"
echo ""

# Setup each reference project
setup_repository \
    "https://github.com/ActiveLook/android-sdk.git" \
    "ActiveLook SDK" \
    "$REFERENCE_DIR/android-sdk"

setup_repository \
    "https://github.com/hammerheadnav/karoo-ext.git" \
    "Karoo Extensions" \
    "$REFERENCE_DIR/karoo-ext"

setup_repository \
    "https://github.com/valterc/ki2.git" \
    "Ki2 Reference Project" \
    "$REFERENCE_DIR/ki2"

echo "============================================"
echo -e "\e[32mAll reference projects set up successfully!\e[0m"
echo ""
echo -e "\e[33mNext steps:\e[0m"
echo -e "  \e[90m1. Build the project: ./gradlew :app:assembleDebug\e[0m"
echo -e "  \e[90m2. To update references later: ./update-references.sh\e[0m"
echo "============================================"

