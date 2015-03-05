# Changelog
All notable changes to this project will be documented in this file.

## 0.4.1 - 2015-01-14
### Added
- Build documentation.

## 0.4 - 2015-01-13
### Added
- ReviewAssistant is now configurable.
- Maximum reviewers, configuration parameter.
- Auto add reviewer, configuration parameter.
- If +2 user is required, configuration parameter.
- Modifier for total review time, configuration parameter.
- Load balancing, configuration parameter.
- Age and limit in the +2 query, configuration parameter.

### Fixed
- Query only looks for changes with +2 code review.
- ReviewAssistant will now try to fill with reviewers until maxReviewers is reached.

## 0.3 - 2015-01-05
### Added
- Automatic adding of reviewer with merge rights.
- Load balancing reviewers based how many changes one is reviewing.
- Loading animation while waiting for review advice.
- Review advice now loads on demand.

### Fixed
- Incompatible cache files are recalculated.

## 0.2.1 - 2014-12-10
### Fixed
- Review advice output rounding bug. Should now round correctly and output right ammount of sessions.

## 0.2 - 2014-12-09
### Added
- Automatic adding of reviewer based on git blame.

## 0.1 - 2014-11-22
### Added
- Basic review time suggestion functionality.
