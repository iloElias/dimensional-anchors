# Contributing

Contributions should be focused, reviewable, and consistent with the topic documents in the [design index](docs/DESIGN_DECISIONS.md). These supersede conflicting requirements in the [original specification](docs/DESIGN.md). Project-facing text must be in English.

## Before making changes

- Check the design specification and existing implementation before proposing a change.
- Do not implement an open design question as if it were settled. Get explicit approval first.
- Keep changes limited to the issue or behavior being addressed; avoid unrelated cleanup.
- Do not add, generate, or edit sprite PNG files. Asset art is supplied by the project owner.
- Do not claim compatibility or test results that have not been verified.

## Commit messages

Use the Conventional Commits format:

```text
<type>(<optional-scope>): <short imperative summary>
```

Use a lowercase type and scope, keep the summary concise, and describe the outcome rather than the process. Use a body when context or a compatibility impact is not clear from the subject. Reference relevant issues in the body or footer.

Allowed types:

- `feat`: add user-facing functionality
- `fix`: correct a defect
- `docs`: change documentation
- `test`: add or update tests
- `refactor`: restructure without changing behavior
- `build`: change build configuration or dependencies
- `chore`: maintenance that does not fit another type

Examples:

```text
docs: document contribution and redistribution rules
fix(room): reject unauthorized room entry
test(access): cover invitation revocation
```

Do not include unrelated changes in one commit. Do not use a commit message to imply approval of an unresolved design decision.

## Pull/merge requests

Open a pull request (or merge request, depending on the hosting platform) against the repository's default branch. A request should address one focused change and include:

- A clear summary of the change and its motivation.
- Related issue or design requirement, when applicable.
- Any user-visible, compatibility, persistence, or security impact.
- Tests or checks actually run, with their results; state clearly when none were run.
- Screenshots or reproduction steps for visual or gameplay changes, when useful.

Before submitting, confirm that:

- The change follows the approved design and does not silently resolve an open question.
- Documentation and tests are updated when behavior changes.
- No unrelated generated files, build outputs, or unapproved assets are included.
- The diff is focused and the request is ready for review.

Submitting a request does not grant permission to redistribute the mod outside the terms of [LICENSE](LICENSE).