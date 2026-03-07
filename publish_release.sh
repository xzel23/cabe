#!/usr/bin/env bash

# Exit on error
set -e

# Change to the project root directory
cd "$(dirname "$0")"

# 1. checks that there are no modified files present in the git repository
echo "Checking for modified files..."
if [[ -n $(git status --porcelain) ]]; then
  echo "Error: Working directory is not clean. Please commit or stash your changes before releasing."
  git status
  exit 1
fi

# 2. runs a clean build and makes sure it passes successfully
echo "Running clean build..."
./gradlew fullBuild

# 3. checks again that there are no modified files (build might update version string in files)
echo "Checking for modified files after build..."
if [[ -n $(git status --porcelain) ]]; then
  echo "Error: Working directory became dirty after build. Build might have updated files (e.g., version strings)."
  echo "Please commit these changes before tagging."
  git status
  exit 1
fi

# 4. creates a tag "v<project version>" (read project version from build scripts)
echo "Extracting project version..."
VERSION=$(grep -E "val projectVersion = " build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
if [[ -z "$VERSION" ]]; then
  echo "Error: Could not extract version from build.gradle.kts"
  exit 1
fi

TAG="v$VERSION"
echo "Creating tag $TAG..."
# Check if tag already exists
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Error: Tag $TAG already exists."
  exit 1
fi
git tag "$TAG"

# 5. pushes to git including tags
echo "Pushing to git..."
git push
git push origin "$TAG"

echo "Successfully created and pushed tag $TAG."
