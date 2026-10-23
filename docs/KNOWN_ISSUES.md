# Known Issues

This file tracks reported behavior requiring investigation. A report is not a
confirmed root cause or evidence that a fix has been implemented.

## DA-002: Extracted registered anchor reported with an unset registration flag

- **Status:** Not reproduced in the owner's follow-up test on 2026-10-23;
  original Gold-anchor report remains unexplained. No corrective code change claimed.
- The key and Gold anchor reference UUID `655131b1-ae5d-4252-90ca-d8529856ad65`,
  but the owner reports `registered:0b` on a freshly survival-extracted anchor.
- The server still rejects another blank key after upgrading and restarting.
- The installed JAR matched the previously tested JAR by SHA-256. Automated
  registration/extraction lifecycle tests pass with `registered=true`, so the
  reported client scenario is not yet reproduced or resolved.
- The owner's fresh Copper tests confirmed `registered:0b` before registration
  (UUID `4c5b7304-5cf9-4b3e-b8c1-0e43439dc5af`) and `registered:1b` after
  registration (UUID `a48c3ec8-f570-4e36-85e8-b6650d67ed85`), as expected.
- If the original scenario recurs, retest a fresh Gold anchor and inspect the
  held item with `/data get entity @s SelectedItem`, not only a tooltip/copy.
  Capture the server result before upgrading, then after restarting and
  extracting again. The expected drop has `registered:1b`.
- Do not reset registration or assign another UUID as a workaround.

## DA-001: Identified anchor remains unavailable after creative destruction

- **Status:** Open; reported by the owner on 2026-10-23. Fix deferred at the
  owner's request.
- **Scope:** An anchor that already has `dimensionalanchors:anchor_data`, used
  with creative infinite inventory. Fresh, unidentified creative items passed
  repeated-placement testing.

### Reproduction

1. Place a new anchor to assign its UUID.
2. Collect it in survival, retaining the identified item.
3. Switch to creative and place that item. The held item is retained.
4. Attempt to place it again: placement is rejected while that UUID is placed.
5. Break the placed block in creative.
6. Attempt to place the retained item again.

### Observed and expected behavior

The owner reports that placement remains blocked after the creative destruction.
Preventing two simultaneously placed anchors with the same UUID is intentional;
continued blocking after removal requires investigation. A future fix must
preserve identity and must not bypass duplicate-placement protection.

### Investigation hypothesis

Creative destruction may bypass the `AnchorBlock.playerDestroy` path that marks
the saved record as absent, leaving `placed=true`. This has not been reproduced
by an automated regression test or confirmed against a server log.

### Future regression coverage

- Place an identified item in creative and reject a second simultaneous placement.
- Destroy the placed block through the actual creative breaking path.
- Verify that its saved record is absent, without deleting its identity.
- Place the retained item again and confirm the same UUID.
- Repeat after saving and reopening the world.
