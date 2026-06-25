#!/usr/bin/env bash

# Exit on error
set -e

# Change to the project root directory
cd "$(dirname "$0")"

# 1. Check for clean working directory
echo "Checking for modified files..."
if [[ -n $(git status --porcelain) ]]; then
  echo "Error: Working directory is not clean. Please commit or stash your changes before releasing."
  git status
  exit 1
fi

# 2. Extract project version from build.gradle.kts
echo "Extracting project version..."
VERSION=$(grep -E "val projectVersion = " build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
if [[ -z "$VERSION" ]]; then
  echo "Error: Could not extract version from build.gradle.kts"
  exit 1
fi
echo "Project version: $VERSION"

# 3. Check for required environment variables
echo "Checking for environment variables..."
REQUIRED_VARS=(
  "JRELEASER_GPG_PUBLIC_KEY"
  "JRELEASER_GPG_SECRET_KEY"
  "JRELEASER_GPG_PASSPHRASE"
  "JRELEASER_GITHUB_TOKEN"
  "SONATYPE_USERNAME"
  "SONATYPE_PASSWORD"
  "GRADLE_PUBLISH_KEY"
  "GRADLE_PUBLISH_SECRET"
)

for var in "${REQUIRED_VARS[@]}"; do
  if [[ -z "${!var}" ]]; then
    echo "Error: Environment variable $var is not set."
    exit 1
  fi
done

# Map JReleaser variables if they are not already set (CI uses both sets)
export SIGNING_PUBLIC_KEY="${JRELEASER_GPG_PUBLIC_KEY}"
export SIGNING_SECRET_KEY="${JRELEASER_GPG_SECRET_KEY}"
export SIGNING_PASSWORD="${JRELEASER_GPG_PASSPHRASE}"

# 4. Run full build and tests
# The 'build' task in build.gradle.kts already includes allTests, shadowJar,
# publishToMavenLocal, publishToStagingDirectory, and various regression tests.
echo "Running full build and tests..."
./gradlew --no-daemon --build-cache clean build

# 5. Create and push tag
TAG="v$VERSION"
echo "Creating tag $TAG..."
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Error: Tag $TAG already exists."
  exit 1
fi
git tag "$TAG"

echo "Pushing to git..."
git push origin main
git push origin "$TAG"

# 6. Publish release
echo "Publishing release via JReleaser..."
./gradlew --no-daemon -Prelease jreleaserDeploy jreleaserUpload

# 7. Publish Gradle Plugin
echo "Publishing Gradle plugin..."
./gradlew --no-daemon -Prelease cabe-gradle-plugin:publishPlugins

echo "Successfully released version $VERSION."