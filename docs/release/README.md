# Abdul Windows Release Docs

Start here for the July 2026 Abdul Windows handoff.

## Release-Facing Docs

- `ABDUL-WINDOWS-RELEASE-REPORT.md` - main human-readable report of bugs, fixes, optimizations, Gradle/native build changes, risks, and test checklist.
- `BUILD-RUN-GUIDE.md` - practical Windows, macOS, and Linux setup and launch guide for volunteers.
- `CONCURRENCY-MRR-FIXES-2026-07-15.md` - source-to-failure explanation and validation record for the MRR and AutoPolyVary concurrency fixes.

## Reference Docs

The `reference/` folder contains copied study material from the wider workspace so this repo can be handed off without relying on external paths:

- `reference/current-tracker/` - current Abdul tracker files from `docs/codex-project-study/`.
- `reference/project-study/` - architecture, comparison, build, math, and plan docs from the earlier project study.
- `reference/bug-audit/` - bug-audit summaries and tracker files.
- `reference/source-study/` - source-study summaries and symbol/function indexes.

Large raw decompiler/Ghidra artifact trees were not copied into this release folder because they are noisy and not useful for volunteer setup. The report names them as evidence sources when relevant.
