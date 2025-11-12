# 9. Automated Release Notes Management for Play Store Deployments

Date: 2025-11-07

## Status

Accepted

## Context

Previously, managing release notes for Play Store deployments was a manual and error-prone process. The workflow lacked automation for:
- Validating that version-specific release notes exist before deployment
- Ensuring release notes comply with Play Store's 500 character limit
- Copying version-specific release notes to the default.txt files required for deployment
- Providing clear feedback about which release notes will be published

This manual process increased the risk of deploying with incorrect or missing release notes, and made it difficult to catch validation errors early in the CI/CD pipeline.

## Decision

We introduced an automated release notes preparation script (`scripts/prepare-release-notes.sh`) that is integrated into our CI/CD workflows. The script:

1. **Extracts the current version** from `AndroidCoordinates.kt` to determine which release notes to use
2. **Validates version-specific files exist** - fails the build if `app/src/main/play/release-notes/{lang}/{version}.txt` files are missing
3. **Copies version files to default.txt** - automatically prepares the files needed for Play Store deployment
4. **Validates character counts** - ensures release notes don't exceed Play Store's 500 character limit
5. **Provides preview** - displays the exact release notes that will be deployed

The script is integrated into:
- `build-production.yml` workflow for production releases
- `build-main-push.yml` workflow for main branch deployments
- `build-develop-push.yml` workflow for develop branch deployments
- `deploy.yml` workflow for general deployments

## Consequences

**Positive:**
- Release notes validation happens early in CI/CD, preventing deployment of apps with missing or invalid release notes
- Character limit validation prevents Play Store submission failures
- Automated copying eliminates manual file management errors
- Clear preview output ensures visibility into what will be deployed
- Version-specific release notes files create a historical record of all releases
- Consistent process across all deployment workflows

**Considerations:**
- Release notes must be created as version-specific files (e.g., `4.16.0.txt`) before deployment
- The script requires Python 3 for character counting functionality
- Failed validation blocks the entire CI/CD pipeline (by design, to prevent bad deployments)
- Changes to version naming in `AndroidCoordinates.kt` must be reflected in release notes file naming

**File Structure:**
```
app/src/main/play/release-notes/
├── en-US/
│   ├── 4.16.0.txt    (version-specific)
│   └── default.txt   (auto-generated)
└── de-DE/
    ├── 4.16.0.txt    (version-specific)
    └── default.txt   (auto-generated)
```