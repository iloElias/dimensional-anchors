# Known Issues

This file tracks reported behavior requiring investigation. A report is not a
confirmed root cause or evidence that a fix has been implemented.

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
