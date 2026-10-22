
# Dimensional Anchors

Dimensional Anchors is a NeoForge mod project for Minecraft 26.1.2. Its design describes upgradeable anchor blocks that hold persistent pocket rooms, accessible only by using a linked access key.

**Project status:** In development. The design specification is authoritative; planned features are not necessarily implemented or available in a release.

See the [progress report](docs/PROGRESS.md) for implemented source changes, validation evidence, and remaining work.

## Compatibility

- Minecraft: `26.1.2`
- NeoForge: `26.1.2.122`
- Java: `25`

## Design

Start with the [design index](docs/DESIGN_DECISIONS.md) for approved decisions organized by topic: anchors, keys, travel, rooms, dimension, recipes, assets, remaining questions, and verification.

The [original specification](docs/DESIGN.md) remains a historical baseline. The topic documents linked from the index take precedence where requirements conflict. Recommendations and open questions are not approved behavior.

Use [Implementation Kickoff](docs/IMPLEMENTATION.md) to begin the technical assessment and plan incremental implementation under the existing approval gates.

## Build

Builds run on GitHub Actions, not locally. Push a release tag starting with `v`
(for example, `v1.0.0`) to trigger the release workflow:

```sh
git tag v1.0.0
git push origin v1.0.0
```

The workflow builds with Java 25, attaches the installable JAR to a GitHub Release,
and includes compatibility information, build provenance, and automatically generated
release notes. Regular branch pushes and pull requests do not trigger this workflow.
The mod version comes from `gradle.properties`; update it before tagging a new version.
Download published builds from the repository's Releases page.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull/merge request. Use English for project-facing documentation, comments, and UI strings. Do not resolve [open design questions](docs/design/OPEN_QUESTIONS.md) without explicit approval.

## Attribution and redistribution

The mod is copyright (c) 2026 Murilo Elias. Redistribution is governed by [LICENSE](LICENSE), not by the NeoForge template license. Public re-uploads must preserve the original author and project attribution, link to the canonical repository, disclose modifications, and comply with this repository's contribution standards. Do not present an unofficial build or fork as the original project.

The canonical repository is [github.com/iloElias/dimensional-anchors](https://github.com/iloElias/dimensional-anchors).
