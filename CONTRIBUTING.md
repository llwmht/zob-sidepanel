# Contributing

- Everything is written in English: UI text, code, comments, commit messages, docs.
- Commit subjects follow Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`), in the
  imperative, under 72 characters. One concern per pull request.
- New logic comes with a unit test. Anything that can only be checked on a device goes into
  [docs/device-checks.md](docs/device-checks.md) with the steps to check it.
- Widget code is plain ES2019 with no build step and no framework.
- Do not add a dependency without saying in the pull request why the platform or an existing
  dependency does not cover it.

## Widget contract

Defined in milestone M2. See [docs/PLAN.md](docs/PLAN.md), section 5.
