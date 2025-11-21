#!/bin/bash
# Update Reference Projects Script
# This script pulls the latest updates from the reference repositories

echo -e "\e[36mUpdating reference projects...\e[0m"
echo ""

HAS_ERRORS=0

# Function to update a git repository
update_repository() {
    local path=$1
    local name=$2

    echo -e "\e[33mUpdating $name...\e[0m"

    if [ -d "$path" ]; then
        pushd "$path" > /dev/null

        # Fetch latest changes
        echo -e "  \e[90mFetching latest changes...\e[0m"
        git fetch origin

        # Get current branch
        current_branch=$(git rev-parse --abbrev-ref HEAD)
        echo -e "  \e[90mCurrent branch: $current_branch\e[0m"

        # Check for local changes
        if [ -n "$(git status --porcelain)" ]; then
            echo -e "  \e[31mWARNING: Local changes detected in $name\e[0m"
            echo -e "  \e[90mStashing local changes...\e[0m"
            git stash save "Auto-stash before update $(date '+%Y-%m-%d %H:%M:%S')"
        fi

        # Pull latest changes
        echo -e "  \e[90mPulling latest changes...\e[0m"
        if git pull origin "$current_branch"; then
            # Show latest commit
            latest_commit=$(git log -1 --pretty=format:"%h - %s (%cr by %an)")
            echo -e "  \e[90mLatest commit: $latest_commit\e[0m"
            echo -e "  \e[32m✓ $name updated successfully\e[0m"
        else
            echo -e "  \e[31m✗ Error updating $name\e[0m"
            HAS_ERRORS=1
        fi

        echo ""
        popd > /dev/null
    else
        echo -e "  \e[31m✗ Repository not found at: $path\e[0m"
        echo -e "  \e[33mRun setup-references.sh to clone the repositories\e[0m"
        echo ""
        HAS_ERRORS=1
    fi
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REFERENCE_DIR="$SCRIPT_DIR/reference"

echo -e "\e[90mReference directory: $REFERENCE_DIR\e[0m"
echo ""

# Update each reference project
update_repository "$REFERENCE_DIR/android-sdk" "ActiveLook SDK"
update_repository "$REFERENCE_DIR/karoo-ext" "Karoo Extensions"
update_repository "$REFERENCE_DIR/ki2" "Ki2 Reference Project"

# Summary
echo "============================================"
if [ $HAS_ERRORS -eq 1 ]; then
    echo -e "\e[31mUpdate completed with errors!\e[0m"
    echo -e "\e[33mPlease review the errors above and fix them manually.\e[0m"
    exit 1
else
    echo -e "\e[32mAll reference projects updated successfully!\e[0m"
    echo ""
    echo -e "\e[33mNext steps:\e[0m"
    echo -e "  \e[90m1. Review changes: git log in each reference directory\e[0m"
    echo -e "  \e[90m2. Rebuild project: ./gradlew clean :app:assembleDebug\e[0m"
    echo -e "  \e[90m3. Test the app to ensure compatibility\e[0m"
fi
echo "============================================"

