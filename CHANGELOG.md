# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- Fixed folders showing up incorrectly as files in copy/move dialog ([#267])
- Fixed error when saving files with unsupported characters ([#250])
- Fixed missing permission prompt on initial "Save as" launch ([#85])
- Fixed printing text files containing a "#" ([#104])
- Fixed files in hidden folders showing up in recent tab ([#217])

### Added
- Added a separate "Save as" option in the text editor ([#224])

### Changed
- Save button now overwrites files directly in the text editor ([#224])

## [1.2.3] - 2025-09-15
### Fixed
- Fixed folders showing up incorrectly as files in some cases ([#80])

## [1.2.2] - 2025-09-01
### Changed
- Updated translations

### Fixed
- Fixed empty storage usage bars on initial load ([#251])

## [1.2.0] - 2025-07-12
### Changed
- Updated translations

### Fixed
- File modification time is now preserved when compressing/decompressing files ([#176])

## [1.1.0] - 2025-05-21
### Added
- Added SHA-1 and SHA-256 hashes to file properties ([#149])

### Changed
- Replaced checkboxes with switches
- Updated translations

### Removed
- Dropped support for Android 7 and older versions

### Fixed
- Files saved via `Save As` option are now named properly ([#37])
- Decompressed folders are now named properly ([#120])
- Text editor no longer resets on device rotation ([#106])
- JSON files can now be edited in-app ([#105])
- Fixed accidental rendering of pull-to-refresh indicator ([#27])
- Pull-to-refresh setting is now applied as expected ([#136])
- Fixed invisible content when using app lock ([#150])

## [1.0.1] - 2024-03-17
### Changed
- Folders now use primary/accent color.
- Enhanced search performance in the text editor.
- Added some translations.

### Fixed
- Addressed unnecessary delays when renaming files.
- Fixed unresponsive behavior when viewing contents of a zip file.
- Disabled the use of invalid characters such as `/` for batch file renaming.
- Fixed invisible sort menu when viewing storage summary.

## [1.0.0] - 2024-01-07
### Added
- Initial release

[#27]: https://github.com/FossifyOrg/File-Manager/issues/27
[#37]: https://github.com/FossifyOrg/File-Manager/issues/37
[#80]: https://github.com/FossifyOrg/File-Manager/issues/80
[#105]: https://github.com/FossifyOrg/File-Manager/issues/105
[#106]: https://github.com/FossifyOrg/File-Manager/issues/106
[#120]: https://github.com/FossifyOrg/File-Manager/issues/120
[#136]: https://github.com/FossifyOrg/File-Manager/issues/136
[#149]: https://github.com/FossifyOrg/File-Manager/issues/149
[#150]: https://github.com/FossifyOrg/File-Manager/issues/150
[#176]: https://github.com/FossifyOrg/File-Manager/issues/176
[#251]: https://github.com/FossifyOrg/File-Manager/issues/251
[#267]: https://github.com/FossifyOrg/File-Manager/issues/267
[#250]: https://github.com/FossifyOrg/File-Manager/issues/250
[#85]: https://github.com/FossifyOrg/File-Manager/issues/85
[#104]: https://github.com/FossifyOrg/File-Manager/issues/104
[#224]: https://github.com/FossifyOrg/File-Manager/issues/224
[#217]: https://github.com/FossifyOrg/File-Manager/issues/217

[Unreleased]: https://github.com/FossifyOrg/File-Manager/compare/1.2.3...HEAD
[1.2.3]: https://github.com/FossifyOrg/File-Manager/compare/1.2.2...1.2.3
[1.2.2]: https://github.com/FossifyOrg/File-Manager/compare/1.2.0...1.2.2
[1.2.0]: https://github.com/FossifyOrg/File-Manager/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/FossifyOrg/File-Manager/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/FossifyOrg/File-Manager/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/FossifyOrg/File-Manager/releases/tag/1.0.0
