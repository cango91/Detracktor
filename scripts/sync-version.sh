#!/bin/bash
set -e

# sync-version.sh - Synchronize version.properties with git tag
# Usage: ./scripts/sync-version.sh v1.2.3

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version-tag>"
    echo "Example: $0 v1.2.3"
    exit 1
fi

TAG_VERSION="$1"

# Remove 'v' prefix if present
VERSION_NAME="${TAG_VERSION#v}"

# Validate version format (basic semver check)
if ! [[ "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-.*)?$ ]]; then
    echo "Error: Version must be in format X.Y.Z or X.Y.Z-suffix"
    echo "Got: $VERSION_NAME"
    exit 1
fi

# Calculate version code from version name (simple increment scheme)
# You can customize this logic based on your needs
IFS='.' read -ra VERSION_PARTS <<< "${VERSION_NAME%%-*}"
MAJOR=${VERSION_PARTS[0]}
MINOR=${VERSION_PARTS[1]}
PATCH=${VERSION_PARTS[2]}

# Simple version code calculation: MAJOR * 10000 + MINOR * 100 + PATCH
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))

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
