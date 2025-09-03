#!/bin/bash
set -e

# sync-version.sh - Synchronize version.properties with git tag
# Usage: ./scripts/sync-version.sh v1.2.3[-alpha.1]

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version-tag>"
    echo "Example: $0 v1.2.3 or $0 v1.2.3-alpha.1"
    exit 1
fi

TAG_VERSION="$1"

# Remove 'v' prefix if present
VERSION_NAME="${TAG_VERSION#v}"

# Regex for advanced semver parsing including pre-release
# This regex is adapted for POSIX compatibility
if [[ ! "$VERSION_NAME" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-(.*))?$ ]]; then
    echo "Error: Version must be in semver format X.Y.Z or X.Y.Z-pre.release"
    echo "Got: $VERSION_NAME"
    exit 1
fi

# Extract components using bash built-in regex matching
MAJOR=${BASH_REMATCH[1]}
MINOR=${BASH_REMATCH[2]}
PATCH=${BASH_REMATCH[3]}
PRE_RELEASE_SUFFIX=${BASH_REMATCH[5]} # Captures the whole pre-release part (e.g., alpha.1)

# Base version code calculation: MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000
# Leave room for pre-release identifiers.
VERSION_CODE=$((MAJOR * 10000000 + MINOR * 100000 + PATCH * 1000))

# Handle pre-release suffixes for version code
# Android's versionCode must be a monotonically increasing integer.
# To ensure pre-release versions are lower than the final release,
# we need to assign a negative offset.
if [[ -n "$PRE_RELEASE_SUFFIX" ]]; then
    # Standardize common pre-release labels and assign them an ordinal value
    # Pre-release versions should sort lower than the main release.
    # Example precedence: 1.2.3-alpha < 1.2.3-beta < 1.2.3-rc < 1.2.3
    case "${PRE_RELEASE_SUFFIX%%.*}" in
        "alpha")
            PRE_RELEASE_INDEX=1
            ;;
        "beta")
            PRE_RELEASE_INDEX=2
            ;;
        "rc")
            PRE_RELEASE_INDEX=3
            ;;
        *)
            # For other pre-release types, use a high index to keep them sorted
            PRE_RELEASE_INDEX=99
            ;;
    esac

    # Extract the pre-release number (e.g., '1' from 'alpha.1')
    PRE_RELEASE_NUMBER=$(echo "$PRE_RELEASE_SUFFIX" | grep -o '[0-9]*$')
    # If no number is found, assume 0
    if [[ -z "$PRE_RELEASE_NUMBER" ]]; then
        PRE_RELEASE_NUMBER=0
    fi

    # Calculate pre-release offset. Higher numbers indicate a later release.
    # The offset is subtracted from the final version code.
    # We use a negative offset to make pre-releases lower than stable releases.
    PRE_RELEASE_OFFSET=$((1000 - (PRE_RELEASE_INDEX * 10) - PRE_RELEASE_NUMBER))
    VERSION_CODE=$((VERSION_CODE + PRE_RELEASE_OFFSET))
fi

echo "Updating version.properties:"
echo "  VERSION_NAME: $VERSION_NAME"
echo "  VERSION_CODE: $VERSION_CODE"

# Update version.properties
cat > version.properties << EOF
# App version configuration
# This file is the single source of truth for version information
VERSION_NAME=$VERSION_NAME
VERSION_CODE=$VERSION_CODE
EOF

echo "Version synchronized successfully!"
echo ""
echo "Next steps:"
echo "1. Review the changes: git diff version.properties"
echo "2. Commit the version update: git add version.properties && git commit -m 'chore: bump version to $VERSION_NAME'"
echo "3. Create the tag: git tag $TAG_VERSION"
echo "4. Push with tags: git push origin main --tags"
