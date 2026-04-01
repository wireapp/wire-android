# Wire Android Documentation

This documentation set describes the `wire-android` repository for engineers who are onboarding to the codebase or preparing retrieval-friendly reference material.

Each document is intentionally scoped, sectioned, and cross-linked so it can be indexed in a RAG pipeline without depending on one large README.

## Documentation Map

- [General Project Description](./general-project-description.md)
- [Project Structure](./project-structure.md)
- [Architecture](./architecture.md)
- [Dependencies](./dependencies.md)
- [Features](./features.md)

## Existing Reference Material

- [Architecture Decision Records](./adr/)
- Root-level project overview: [`README.md`](../README.md)
- Contribution guide: [`CONTRIBUTING.md`](../CONTRIBUTING.md)

## How To Use This Documentation

Use [General Project Description](./general-project-description.md) to understand what the repository contains and how it fits into Wire as a product and open-source project.

Use [Project Structure](./project-structure.md) to locate modules, included builds, and top-level responsibilities.

Use [Architecture](./architecture.md) to understand runtime flow, dependency injection, navigation, background processing, and the boundary with `kalium`.

Use [Dependencies](./dependencies.md) to understand build tooling, first-party module relationships, included-build substitution, and major third-party libraries.

Use [Features](./features.md) to map end-user capabilities and platform capabilities to app packages and feature modules.
