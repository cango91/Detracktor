#!/bin/bash
# setup-hooks.sh - Install git hooks for version management

set -e

echo "Setting up git hooks for version management..."

# Install pre-push hook
if [ -f "hooks/pre-push" ]; then
    cp hooks/pre-push .git/hooks/pre-push
    chmod +x .git/hooks/pre-push
    echo "✅ Installed pre-push hook for version validation"
else
    echo "❌ hooks/pre-push not found"
    exit 1
fi

echo ""
echo "🎉 Git hooks installed successfully!"
echo ""
echo "Usage:"
echo "1. Update version: ./scripts/sync-version.sh v1.2.3"
echo "2. Commit changes: git add . && git commit -m 'chore: bump version to 1.2.3'"
echo "3. Create tag: git tag v1.2.3"
echo "4. Push: git push origin main --tags"
echo ""
echo "The pre-push hook will validate version synchronization automatically."
