# Progress Report

**Date:** 2026-10-22  
**Last updated:** 2026-10-23 (UTC-03:00)\
**Configured version:** `0.1.0-alpha.1` (release tag: `v0.1.0-alpha.1`)  
**Stage:** Identity and upgrade regressions compiled and headless-tested; client/JEI validation pending.

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
- Configured anchor sounds using the corresponding vanilla material block's
  sound type: Copper, Iron, Gold, Diamond, and Netherite block sounds. No
  custom audio files or sound events are introduced.
- Configured anchors with material block hardness and approved ore-equivalent
  blast resistance, requiring an iron-tier or better pickaxe to mine.
- Added pickaxe mining tags and block loot tables so the correct tool is
  recognized and anchors drop their corresponding item when harvested.
- Configured boundary and exit blocks with bedrock hardness and blast
  resistance so they cannot be broken in survival.
- Limited dimensional key stacks to one item.
- Registered the approved catalyst, anchor progression, template, Netherite
  smithing, and blank-key recipes.
- Internal preview blocks currently use Stone sounds.

The two key items and creative access to internal blocks are preview conveniences,
not a final implementation of key states or survival availability.

### Persistence foundation

- Added validated anchor tiers and persistent item-data records for anchor
  identities and registered-key references.
- Registered synchronized data components for anchor item data and key data.
- Added server-side `SavedData` records keyed by anchor UUID, including tier,
  registration, placement state, exterior position, and data version.
- Wired anchor placement to create a UUID when needed and persist its position.
- Wired player extraction to return the same UUID and preserve registration data.
- Upgrade recipes now preserve identity, registration, and other item components.
- Key registration is not wired yet.

### Netherite recipe synchronization fix

- The client log reported a `neoforge:recipe_content` encoding failure on world
  entry: the Netherite recipe's JSON and network unit codecs used different
  instances, which failed the network codec's equality check.
- Initially fixed by sharing a single recipe instance between the unit codecs.
- The upgrade regression fix below supersedes that implementation with vanilla
  smithing codecs; JSON/network round trips are now covered by a running test.

### Anchor upgrade and stack regressions

- Replaced the crafting matcher that rejected its own center slot with vanilla
  shaped recipe matching. All four upgrades retain vanilla display and placement
  metadata, and use vanilla recipe subclasses recognized by JEI.
- Result previews preserve existing UUIDs, registration, and other components.
  Pristine anchors remain unidentified until placement; previews allocate no UUID.
- Placement no longer writes identity onto the held stack. The server assigns
  each newly placed block its own UUID, leaving remaining survival/creative items
  unchanged. Failed placement consumes nothing and creates no item data.
- Replacing an upgraded anchor updates the saved tier, preventing extraction from
  reverting the anchor to its previous item tier.
- Added four headless regression GameTests in the separate `gameTest` source set,
  excluded from the production JAR, and a release-workflow gate to run them.
- Tests cover all upgrade tiers, invalid grids/materials, deterministic previews,
  registered/unregistered identity transfer, JSON/network codecs, display metadata,
  five consecutive placements in survival and creative, blocked placement,
  upgraded drops, and saved-data serialization.
- Existing stacks contaminated by the earlier shared-UUID bug are not
  automatically reassigned identities. Use freshly crafted stacks for retesting;
  do not duplicate an identified anchor to create independent anchors.

### Visual resources

- Added seven full-cube block models and their blockstates.
- Added client item definitions for the five anchors, the boundary and exit
  blocks, and four standalone items.
- Added four flat item models using the owner-supplied textures.
- Preserved the supplied PNG artwork without editing it.

### Release automation

- Changed the [workflow](../.github/workflows/build.yml) to run only on pushed
  tags matching `v*`, following the reference modpack's release pattern.
- Configured Java 25 builds on GitHub Actions.
- Configured release publication with installable JAR attachments, compatibility
  information, installation instructions, build provenance, and generated notes.
- Set the initial prototype version to `0.1.0-alpha.1`. Tags containing a hyphen
  are published as GitHub pre-releases; titles and descriptions are automatic.
- The expected JAR name is `dimensionalanchors-0.1.0-alpha.1.jar`, enforced by
  Gradle and checked by the release workflow from the mod ID and configured
  version.
- The release title uses the tag name. Its description combines generated
  compatibility/installation information with GitHub's automatic release notes;
  manual title or description entry is not required.
- Configured the release workflow to derive the Gradle and release-note version
  from the pushed tag, removing dependence on the static `mod_version` value in
  `gradle.properties`.
- Documented the rule against local compilation in
  [CONTRIBUTING](../CONTRIBUTING.md) and the [README](../README.md).

## Validation evidence

| Check | Result |
| --- | --- |
| Parse asset JSON and check local model/texture references | Passed for 28 asset JSON files at the time of the check; this does not establish registry coverage or successful game loading |
| Parse workflow YAML, check tag-only trigger, and check shell syntax | Passed |
| Execute release-information generation with simulated GitHub environment variables | Passed |
| Parse updated workflow YAML and inspect pre-release classification and configured version | Passed; alpha, beta, and release-candidate tag examples contain a hyphen and are classified as pre-releases; a stable tag example is not |
| `git diff --check` on modified workflow/documentation/assets | No whitespace errors reported; Git emitted an existing invalid-attribute warning from `.gitattributes` |
| Initial local compilation attempt | Not completed: direct wrapper execution was denied; retry via Bash ended with exit code 143 during Minecraft decompilation |
| Authorized local regression run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build` | Passed; four mod regression tests and one vanilla test passed; production JAR built successfully |
| GitHub Actions compilation and release publication | Not run in this session |
| Owner's in-game report, 2026-10-23: full upgrade progression and extraction | Passed; UUID `00374300-47de-448e-8721-8097b10908f3` retained through Iron, Gold, Diamond, and Netherite; tier updated and registration remained false |
| Owner's in-game report: save, leave, reopen, and extract | Passed; Copper UUID `8d4343c1-258d-48cd-ada4-6f276b7e23fb` retained |
| Owner's in-game report: JEI, shift-click crafting, Netherite consumption | Recipes visible again; no shift-click duplication observed; one template, ingot, and Diamond anchor consumed |
| Owner's in-game report: independent/stacked anchors and fresh creative placement | Distinct UUIDs confirmed; fresh creative items reusable; identity assigned only on first placement, regardless of tier |
| Owner's in-game report: previously identified item after creative destruction | Open edge case: retained item remains blocked after its placed block is destroyed; see [DA-001](KNOWN_ISSUES.md#da-001-identified-anchor-remains-unavailable-after-creative-destruction) |
| Sound checks and dedicated-server multiplayer client synchronization for these fixes | Not performed in the reported checkpoint |

Local compilation and headless testing were explicitly authorized by the owner
for this regression task. Client results above were reported by the owner, not
executed by the assistant. No published release or broader mod compatibility is
claimed; the normal restriction on local compilation remains.

## Not implemented

- Anvil key copying and registration, including preservation of key data.
- Key registration, copying, charge, cooldown, and teleportation.
- Room identity, persistence, allocation, generation, and expansion.
- Shared dimension and custom biome.
- Protected boundaries, working exit controls, and movement enforcement.
- Destruction lifecycle handling and offline-player session handling.

## Remaining decisions and artwork

- Recovery after permanent loss of an anchor.
- Allocation capacity and behavior when capacity is exhausted.
- Remaining custom-biome details and final artwork.
- The owner plans to make the blank and registered key sprites visually distinct.

See [open questions](design/OPEN_QUESTIONS.md) for the authoritative list.
Reported defects and investigation candidates are tracked in
[known issues](KNOWN_ISSUES.md).

## Next checkpoint

1. The owner confirmed the upgrade/identity client checkpoint. Keep the identified
   creative-destruction edge case tracked as DA-001; its fix is deferred.
2. On approval to proceed, add key registration using the persisted anchor identity.
3. Commit and push the intended changes when requested; the configured initial
   version is `0.1.0-alpha.1`. Create and push `v0.1.0-alpha.1` only when approved.
4. Let the tag-triggered GitHub workflow compile and publish the artifact.
5. Resolve any CI failures before claiming a successful build.
6. Test all eleven creative entries, placed blocks, textures, names, and sounds
   in a disposable world.
7. Record actual results here before starting room and teleport implementation.
