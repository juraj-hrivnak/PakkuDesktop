<div align="center">

<p align="center">
  <img src="site/images/home-dark-v1.0.0-alpha.11.png#gh-light-mode-only" alt="..." width="840">
  <img src="site/images/home-light-v1.0.0-alpha.11.png#gh-dark-mode-only" alt="..." width="840">
</p>

# Pakku Desktop

The desktop app for developing Minecraft modpacks.<br/>
Built on the [Pakku](https://github.com/juraj-hrivnak/Pakku) Kotlin API.

[![Release](https://img.shields.io/github/v/release/juraj-hrivnak/PakkuDesktop?include_prereleases&sort=semver&label=alpha)](https://github.com/juraj-hrivnak/PakkuDesktop/releases)
[![Platforms](https://img.shields.io/badge/platforms-Windows%20%C2%B7%20macOS%20%C2%B7%20Linux-5b5f66)](https://github.com/juraj-hrivnak/PakkuDesktop/releases)
[![Discord](https://img.shields.io/badge/Discord-join%20the%20chat-5865F2?logo=discord&logoColor=white)](https://discord.com/invite/dtAyqdzTMj)

[Download](#download) · [Features](#features) · [Documentation](#documentation) · [Build from source](#build-from-source)

</div>

> [!IMPORTANT]
> Pakku Desktop is in alpha. New builds ship regularly and things will change and occasionally break.
> Try it, break it, and report what you find in [issues](https://github.com/juraj-hrivnak/PakkuDesktop/issues) or on [Discord](https://discord.com/invite/dtAyqdzTMj).

## About

Pakku Desktop brings modpack development the workflow of a real project. Your modpack is not a JAR pile in a launcher instance: it is plain files under version control, easy to review, easy to collaborate on, and ready for CI/CD.

Pakku Desktop is not a wrapper around the Pakku CLI: it is built directly on the [Pakku](https://github.com/juraj-hrivnak/Pakku) Kotlin API. The modpack format, projects and actions behave identically in both, so you can switch between the GUI and the CLI at any time, even in the same project. Use it alongside the launcher of your choice: Pakku Desktop develops the modpack, your launcher plays it.

## Download

Prebuilt binaries are published on the [releases page](https://github.com/juraj-hrivnak/PakkuDesktop/releases) for all three major platforms.

| Platform | Formats |
| --- | --- |
| Windows (x64, arm64) | `.msi`, installer `.exe`, portable `.exe` |
| macOS (Intel, Apple Silicon) | `.dmg`, `.zip` |
| Linux (x86_64, arm64) | `.deb`, `.rpm`, `.pacman`, `.AppImage`, `.snap` |

## Features

### From the Pakku engine, shared with the CLI

- **Project operations.** Add, update, and remove projects from CurseForge, Modrinth, or both simultaneously, with automatic dependency resolution and safe-removal dependency checking.
- **Development environment.** Fetch project files into your modpack folder and sync local changes back into the modpack.
- **Export.** Build distribution-ready packages for CurseForge and Modrinth, with a report of each profile's size and duration.
- **Version pinning.** Set a per-project update strategy (`LATEST`, `FLEXVER` or `NONE`) without hand-editing the config.
- **Property overrides.** Correct project properties, such as a wrong side, without hand-editing the config.

### Only in Pakku Desktop

- **Projects and Files views.** Browse your modpack, inspect projects, and see every project file visually, including nested dependencies.
- **Plan-reviewed actions.** Every change builds a plan you review before anything is applied.
- **Git built in.** A changes tree, commits, and unified diffs without leaving the app.
- **Plugin API.** Extend the app with your own integrations. UI extension points are currently WIP.

### In the CLI only

Some workflows still live in the CLI, which remains fully supported:

- `pakku remote` to install a modpack straight from a Git URL.
- Automation: the same project runs headless in scripts and CI/CD pipelines, on servers or anywhere without a display.

Use it alongside the launcher of your choice. Use Pakku Desktop to develop the modpack and your launcher to play it.

## Documentation

The modpack format and the Pakku CLI are documented in the [Pakku documentation](https://juraj-hrivnak.github.io/Pakku/).

## Build from source

You will need a JDK; the exact toolchain is defined by the Gradle build. The project uses a git submodule for the Pakku dependency, so clone recursively:

```bash
git clone --recursive https://github.com/juraj-hrivnak/PakkuDesktop.git
cd PakkuDesktop
./gradlew build
```

If you've already cloned without `--recursive`, initialize the submodule first:

```bash
git submodule update --init --recursive
```

On Windows, use `gradlew.bat` instead. If you use Nix, a dev shell with the required native libraries is provided (`nix develop`).

## Built with

- [Kotlin](https://kotlinlang.org)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
- [Jewel](https://github.com/JetBrains/intellij-community/tree/master/platform/jewel)

## License

<!-- TODO: commit a LICENSE file and update this section. Without one, all rights are reserved by default.
     EUPL-1.2-or-later matches the Pakku CLI and the rest of the ecosystem. -->

Not yet decided. A license file will be added before the first stable release.
