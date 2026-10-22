# Progress Report

**Date:** 2026-10-22  
**Stage:** Visual prototype prepared; compilation and in-game validation pending.

This report distinguishes changes present in the working tree from verified gameplay.
It does not replace the [approved design](DESIGN_DECISIONS.md) or imply that the
planned room mechanics are implemented.

## Completed in source

### Design documentation

- Organized approved decisions into [topic documents](DESIGN_DECISIONS.md).
- Documented recipes, key usage, room sizes, shared dimension, expansion, and assets.
- Simplified the anchor lifecycle: collecting or destroying an anchor preserves its
  room and occupants, but suspends new entries while the anchor is absent.
- Prepared the [implementation guide](IMPLEMENTATION.md) and
  [verification matrix](design/VERIFICATION.md).

### Visual prototype

- Added registration code for five anchor blocks: Copper, Iron, Gold, Diamond,
  and Netherite.
- Added registration code for boundary and exit blocks, plus item forms for all
  seven blocks.
- Added catalyst, upgrade template, blank key, and registered-key preview items.
- Defined a **Dimensional Anchors** creative tab with all eleven preview entries,
  using the Copper anchor as its icon.
- Added English display names.
- Configured anchor sounds using the corresponding vanilla ore block's sound
  type; Netherite uses Ancient Debris. No custom audio files or sound events
  are introduced.
- Internal preview blocks currently use Stone sounds.

The two key items and creative access to internal blocks are preview conveniences,
not a final implementation of key states or survival availability.

### Visual resources

- Added seven full-cube block models and their blockstates.
- Added client item definitions for the five anchors and four standalone items.
- Added four flat item models using the owner-supplied textures.
- Preserved the supplied PNG artwork without editing it.

**Known gap:** the internal boundary and exit block items do not yet have client
item definitions. Their inventory rendering needs to be completed before the
visual prototype can be considered ready.

### Release automation

- Changed the [workflow](../.github/workflows/build.yml) to run only on pushed
  tags matching `v*`, following the reference modpack's release pattern.
- Configured Java 25 builds on GitHub Actions.
- Configured release publication with installable JAR attachments, compatibility
  information, installation instructions, build provenance, and generated notes.
- Set the initial prototype version to `0.1.0-alpha.1`. Tags containing a hyphen
  are published as GitHub pre-releases; titles and descriptions are automatic.
- Documented the rule against local compilation in
  [CONTRIBUTING](../CONTRIBUTING.md) and the [README](../README.md).

## Validation evidence

| Check | Result |
| --- | --- |
| Parse asset JSON and check local model/texture references | Passed for 28 asset JSON files at the time of the check; this does not establish registry coverage or successful game loading |
| Parse workflow YAML, check tag-only trigger, and check shell syntax | Passed |
| Execute release-information generation with simulated GitHub environment variables | Passed |
| `git diff --check` on modified workflow/documentation/assets | No whitespace errors reported; Git emitted an existing invalid-attribute warning from `.gitattributes` |
| Initial local compilation attempt | Not completed: direct wrapper execution was denied; retry via Bash ended with exit code 143 during Minecraft decompilation |
| GitHub Actions compilation and release publication | Not run in this session |
| Client launch, inventory rendering, placement, and sound checks | Not performed |

No successful compilation, playable build, or published release is claimed.
Further mod compilation must run on GitHub Actions, not locally.

## Not implemented

- Crafting, smithing, and anvil recipes, including preservation of anchor data.
- Key registration, copying, charge, cooldown, and teleportation.
- Room identity, persistence, allocation, generation, and expansion.
- Shared dimension and custom biome.
- Protected boundaries, working exit controls, and movement enforcement.
- Extraction/destruction lifecycle handling and offline-player session handling.
- Survival mining requirements, loot tables, and relevant tags.

## Remaining decisions and artwork

- Recovery after permanent loss of an anchor.
- Allocation capacity and behavior when capacity is exhausted.
- Remaining custom-biome details and final artwork.
- The owner plans to make the blank and registered key sprites visually distinct.

See [open questions](design/OPEN_QUESTIONS.md) for the authoritative list.

## Next checkpoint

1. Complete missing preview item definitions and review registration against the
   exact Minecraft/NeoForge APIs.
2. Commit and push the intended changes when requested; update the mod version
   before creating an approved release tag.
3. Let the tag-triggered GitHub workflow compile and publish the artifact.
4. Resolve any CI failures before claiming a successful build.
5. Test all eleven creative entries, placed blocks, textures, names, and sounds
   in a disposable world.
6. Record actual results here before starting gameplay implementation.
