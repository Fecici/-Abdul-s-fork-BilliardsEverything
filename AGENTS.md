# Abdul Windows Agent Notes

This repo is the active Abdul Windows source. Future agents working here must treat
`docs/codex-project-study/bug-register.csv` as the live issue tracker and must keep
release-facing changes documented in code and in `docs/release/`.

Working rules:

- Preserve Abdul Windows behavior unless the user explicitly asks to port a NiShan Linux or Suryansh Mac change.
- Do not port the Linux database relocation without a separate migration plan.
- Keep user runtime memory defaults in `build.gradle`: `-Xms2g`, `-Xmx6g`, and `MaxDirectMemorySize=2g`.
- Add short comments for non-obvious code changes, especially Java/native ownership, threading, cancellation, memory, and math behavior.
- Effective 19/07/2026, every future hand-written code change must have a nearby comment in the file's syntax using the exact form `abdul dd/mm/yyyy [what changed and why]`. Use one comment per cohesive changed block, add the comment in every affected code file, preserve older Abdul comments, and add a new dated comment when later behavior changes the same block. Tests, build logic, and hand-written scripts count as code; generated files and formatting-only edits are exempt. Record deletions, which cannot carry a nearby comment, in the live change register and release report.
- After source edits, run at least `.\gradlew.bat --no-daemon compileJava backendSharedLibrary` on Windows.
- For native changes, also run `.\gradlew.bat --no-daemon testBackend` when feasible.
- For Java-only changes, run `.\gradlew.bat --no-daemon test` when feasible.
- Update `docs/codex-project-study/bug-register.csv` for every bug, optimization, build fix, and release feature.
- Keep generated runtime files out of new commits unless the user explicitly wants to commit refreshed artifacts.
