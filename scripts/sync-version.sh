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

# Compatibility offset to handle already published versions
# This ensures new versions are higher than the faulty v1.1.0-alpha.1 = 10100989
COMPATIBILITY_OFFSET=2000

# Handle pre-release suffixes for version code
# Android's versionCode must be a monotonically increasing integer.
# Strategy: Use negative offsets from base to ensure stable > rc > beta > alpha > unknown
# while maintaining proper ordering within each type.
if [[ -n "$PRE_RELEASE_SUFFIX" ]]; then
    # Standardize common pre-release labels and assign them priority values
    # Higher priority = higher version code (closer to stable release)
    case "${PRE_RELEASE_SUFFIX%%.*}" in
        "alpha")
            PRE_RELEASE_PRIORITY=1
            ;;
        "beta")
            PRE_RELEASE_PRIORITY=2
            ;;
        "rc")
            PRE_RELEASE_PRIORITY=3
            ;;
        *)
            # For other pre-release types, use lowest priority
            PRE_RELEASE_PRIORITY=0
            ;;
    esac

    # Extract the pre-release number (e.g., '1' from 'alpha.1')
    PRE_RELEASE_NUMBER=$(echo "$PRE_RELEASE_SUFFIX" | grep -o '[0-9]*$')
    # If no number is found, assume 1
    if [[ -z "$PRE_RELEASE_NUMBER" ]]; then
        PRE_RELEASE_NUMBER=1
    fi
    # Cap at 99 to prevent overflow
    if [[ $PRE_RELEASE_NUMBER -gt 99 ]]; then
        PRE_RELEASE_NUMBER=99
    fi

    # Calculate pre-release offset using the formula:
    # Base - (1000 - PRIORITY * 100 - NUMBER) + COMPATIBILITY_OFFSET
    # This ensures: stable > rc > beta > alpha > unknown
    # and within each type: higher numbers get higher codes
    PRE_RELEASE_OFFSET=$((1000 - (PRE_RELEASE_PRIORITY * 100) - PRE_RELEASE_NUMBER))
    VERSION_CODE=$((VERSION_CODE - PRE_RELEASE_OFFSET + COMPATIBILITY_OFFSET))
else
    # Stable release: add compatibility offset and ensure it ends in 0
    VERSION_CODE=$((VERSION_CODE + COMPATIBILITY_OFFSET))
    # Ensure stable releases end in 0 (industry standard)
    LAST_DIGIT=$((VERSION_CODE % 10))
    if [[ $LAST_DIGIT -ne 0 ]]; then
        VERSION_CODE=$((VERSION_CODE + (10 - LAST_DIGIT)))
    fi
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
