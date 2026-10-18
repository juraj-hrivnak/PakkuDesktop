# Changelog

## Unreleased

## v1.0.0-alpha.8

- Implemented app auto-updating
- Implemented modpack overview tab with project overrides support
- Added open modpack folder button to titlebar
- Added project overrides to modpack model with dedicated tab
- Improved modpack overview and project property overrides UI
- Improved slug editing in ModpackMetaSelection
- Improved style of ProjectOverrideCard and ProjectCard
- Fixed crash in ListImpl and lazy column
- Fixed empty label in Files tab filter
- Fixed project property overrides handling
- Fixed race condition when writing to pakku.json
- Disabled `syncPrimaryDirectories` in FetchImpl.kt

## v1.0.0-alpha.7

- Implemented Git init workflow
- Implemented Git log tab with split panes and scroll bars
- Added search/filter for Git changelist tree
- Added tag support: show tag names in commits, order by creation date, reveal toggle in dropdown
- Added close button to Git log tab diff viewer
- Added success feedback for push operations
- Improved tag handling and modal texts
- Improved main view transitions with AppFrame.kt
- Fixed git hunk header still showing code snippets
- Fixed tag-only push not working
- Warn about update conflicts
- Push deleted remote tags
- Log git commands

## v1.0.0-alpha.6

- Improved git proformance & clean up code
- Improved git diff hunk display
- Removed git branch component form git tab
- Simplified commit FABs
- Implemented diff view soft wrap toggle button
- Fixed nested dependecies UI in modals
- Fixed Git actions lagging the UI thread
- Fixed title bar clipping out of window on bigger UI scales
- Fixed memory leak in Git driver
- Fixed IntelliJ error with compose metrics enabled

## v1.0.0-alpha.5

- Initial alpha release of Pakku Desktop.
- Previous alpha versions are results of packaging testing.
