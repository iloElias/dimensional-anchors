# Dimensional Anchors - Design Decisions Index

The approved decisions previously collected in this file are now organized by topic. This index remains the entry point for existing links.

## Authority and status

- The topic documents below supersede conflicting requirements and open questions in the [original design specification](DESIGN.md).
- Requirements not superseded remain applicable. The original specification is retained as a historical baseline, not a reason to reintroduce replaced behavior.
- In particular, key-based authorization replaces ownership/invitations; keys alone provide entry; the floor stays fixed; and the approved recipes replace the original recipe proposals.
- Recommendations, conditional features, and unresolved technical choices are not approved gameplay.
- Documentation approval is not approval to implement or run the game. No implementation or passing-test status is implied.
- Project-facing documentation remains in English.

## Topics

| Document | Scope |
| --- | --- |
| [Anchor blocks and lifecycle](design/ANCHORS.md) | Identity, activation, extraction, relocation, room preservation, blast resistance, and lifecycle table |
| [Dimensional keys](design/KEYS.md) | Authorization, charging, cooldown, registration, anvil copies, loss, and visual states |
| [Travel and return](design/TRAVEL.md) | Normal exit, room-to-room travel, safe fallbacks, and disconnected players |
| [Room geometry and boundaries](design/ROOMS.md) | Tier sizes, fixed-floor expansion, south-wall door, entry, and boundary deaths |
| [Dimension and compatibility](design/DIMENSION.md) | Shared allocation, first-access generation, End-like environment, chunks, modded content, and entities |
| [Approved recipes](design/RECIPES.md) | Catalyst, all anchor tiers, upgrade template, blank key, and preservation requirements |
| [Catalog and assets](design/ASSETS.md) | Added items/blocks, distinct key sprites, owner-supplied PNGs, and resource conventions |
| [Remaining questions](design/OPEN_QUESTIONS.md) | Outstanding approvals and version-specific technical planning |
| [Workflow and verification](design/VERIFICATION.md) | Approval gates and consolidated future test matrix |

Start with the relevant topic and consult remaining questions before implementation. Do not treat superseded baseline questions as unresolved again.

For the read-only assessment and staged implementation handoff, see [Implementation Kickoff](IMPLEMENTATION.md). It does not replace the approval gates or approve unresolved behavior.
