# Build instructions

## Overview

Tyrant is a single-module Maven project targeting Java 11. The build produces a
runnable jar with `mikera.tyrant.QuestApplication` as the main class.

## Requirements

- JDK 11 or newer
- Maven 3.6+
- A graphical display (or `xvfb` on headless Linux) — some tests exercise AWT

## Build

```bash
mvn clean install
```

This compiles, runs the full test suite (~220 tests) and installs
`tyrant-<version>.jar` into the local Maven repository. The same jar is left in
`target/`.

## Test

```bash
mvn test              # unit tests
mvn verify            # build + tests, without install
```

On headless machines:

```bash
xvfb-run -a mvn verify
```

A handful of tests initialise `QuestApp` and therefore require a display —
without `xvfb` they fail with `java.awt.HeadlessException`.

## Run

```bash
java -jar target/tyrant-1.0.0-SNAPSHOT.jar
```

Or, from within an IDE, run `mikera.tyrant.QuestApplication`.

Optional: create an empty file called `mikeradebug` in the working directory to
enable debug cheats.

## CI Workflows

GitHub Actions workflows live under `.github/workflows/`.

### Build (`ci.yml`)

Runs on every push and pull request against `develop` and `master`. Sets up
Temurin JDK 11, caches the Maven repository, and runs `xvfb-run -a mvn -B verify`.

### Release (`release.yml`)

Triggered when a version tag is pushed (e.g. `v1.0.1`). Steps:

1. Strip the leading `v` from the tag to derive the Maven version.
2. `mvn versions:set -DnewVersion=<version>` to stamp the POM.
3. `xvfb-run -a mvn -B verify` to build and test.
4. `softprops/action-gh-release@v2` creates a GitHub Release with
   auto-generated release notes and attaches `target/tyrant-<version>.jar`.

## Release process

1. Ensure a clean build on `develop`:

   ```bash
   mvn -B clean install
   ```

2. Pick the next version (e.g. `1.0.1`). Releases are cut directly from
   `develop`; `master` is updated by merging the tagged commit in afterwards.

3. Tag and push:

   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

4. The Release workflow builds, tests and publishes
   `tyrant-1.0.1.jar` to the GitHub Releases page.

5. Optionally merge the tagged commit into `master`:

   ```bash
   git checkout master
   git merge --ff-only v1.0.1
   git push origin master
   ```
