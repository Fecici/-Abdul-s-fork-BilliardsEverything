# Handbook Rewrite Work State

Last updated: 2026-07-18

## Authority

- Active source: Abdul Windows fork.
- Reader-facing book: `docs/CODEBASE-HANDBOOK.md`.
- Reproducible reader editions: `tools/docs/build-handbook.ps1` generates the
  ignored `build/handbook/CODEBASE-HANDBOOK.html` and `.pdf` artifacts.
- Source baseline: branch `main`, commit
  `d2a8e6939807b9053f77596b22d005a3ebe111a0`, plus the currently documented
  uncommitted concurrency/test/tooling work.
- Coverage ledger: `docs/handbook/coverage.csv`.
- Generated inventories: `build/doc-audit/`; they are evidence, not prose.

## Completion Contract

A symbol name or generated row is zero explanatory credit. A behavior-bearing
symbol is complete only after its handbook entry explains purpose, contract,
source-order behavior, meaningful branches, invariants, callers/callees, a
worked example, failure modes, and optimization constraints. Trivial symbols
must be grouped with a written rationale.

## Current Gate

- Gate: 10, full explanatory audit and sampled source-to-book review.
- Status: completed for the current 4,063-symbol production inventory.
- Current authoritative checkpoint: handbook Sections 14.125 through 14.181
  complete `Viewer.java`, all remaining native sources and headers, exact
  geometry, native XYZ, the residual Java code/storage generic signatures, the
  dormant/active Java Cover models, the complete active Cover artifact parser,
  Java database administration, and the two legacy Picture carriers. The
  Doxygen-enabled 2026-07-18 audit is 4,063 / 4,063 production symbols (100%):
  2,793 reviewed entries plus 1,270 justified trivial groupings, with 66
  excluded inventory rows and zero explanatory gaps. Recent findings extend
  through BUG-284 and OPT-026. No production or test source was changed by this
  handbook continuation.
- Completed layers: Gates 1-10 now cover mathematical vocabulary, code-number
  effects, Pattern/Expando, symbolic equations, unfolding, bounding and
  refinement, proof/cover/triples/Vary/database/JNA/concurrency, Java/UI
  pipeline dossiers, every remaining source-order symbol, and the final
  explanatory/quality audit.
- Historical checkpoints from Sections 14.28-14.177 remain useful provenance,
  but none is the resume boundary. The only authoritative continuation state
  is the zero-gap Section 14.181 checkpoint above.
- Next exact unresolved source boundary: none in the current explanatory
  inventory. A resumed handbook task should start with reader-driven editorial
  revision or a source change that makes ledger hashes stale, not another blind
  symbol pass. The later instructions PDF remains a separate agenda item.
- Preserve the distinction between mathematical empty, disproved, verified,
  unsupported, cancelled, and backend failure across C++/JNA/Java.
- Production code/test changes are user-authorized, but the completed handbook
  pass intentionally changed documentation/ledgers only; source findings are
  recorded in the bug register before any separate implementation pass.

## Gate Order

1. C++ vocabulary and mathematical representations. **Completed.**
2. Code sequences and the exact effect of changing code numbers. **Completed.**
3. Pattern and Expando. **Completed.**
4. Symbolic algebra and trigonometric equations. **Completed.**
5. Unfolding, shooting vectors, and equation construction. **Completed.**
6. Bounding inequalities and regions. **Completed.**
7. Interval evaluation, Newton/intersection, and exhaustive refinement cases.
   **Completed.**
8. Verification, cover, triples, Vary/search, database, JNA, and concurrency.
   **Completed.**
9. Minimal Java/UI semantic pointers and end-to-end pipeline dossiers.
   **Completed.**
10. Full explanatory audit and sampled human review. **Completed.**

## Final Quality Gate

- Inventory audit with Doxygen: 4,063 / 4,063, zero gaps.
- Ledger arithmetic: 2,793 `reviewed` + 1,270 `grouped-trivial` = 4,063;
  66 nonproduction rows are excluded.
- Sampled source-to-book review covered symbolic math, refinement, cover
  recursion, native ABI ownership, Java Storage, cancellation/progress,
  Viewer workflows, dormant prototypes, exact geometry, Cover artifacts, and
  database administration.
- The sample found and corrected the exact slash/space behavior in
  `CoverStuff` prose. It also normalized 120 corrupted/curly quote sequences;
  the handbook now contains zero non-ASCII and zero mojibake characters.
- Doxygen completed with 25 nonfatal template/member-matching warnings (eight
  each in `unfolding.cpp`/`unfolding.hpp`, four each in
  `evaluator.cpp`/`evaluator.hpp`, and one typedef-signature warning in
  `triangle_billiard4.cpp`). They are retained in
  `build/doc-audit/doxygen-warnings.log`; the source/ctags ledger remains the
  authority for those template specializations.
- `bug-register.csv` parses as 318 unique rows and ends at BUG-284. Every bug,
  optimization, and architecture ID referenced by the handbook exists in the
  register; BUG-281 through BUG-284 were added during the final Java pass.
- A direct handbook whitespace scan finds only four intentional trailing-space
  lines: two Markdown hard breaks in the header and two literal trailing-space
  examples in the native Vary transport grammar. Existing unrelated dirty
  worktree files were not reverted or modified by this documentation
  continuation.

## Reader Editions

- The Markdown file remains authoritative. `handbook-render.lua` removes only
  its duplicate source title for publication, promotes the remaining heading
  hierarchy without changing anchors, and fails the build if TeX appears
  outside supported math delimiters where HTML would silently omit it.
- `handbook-print.css` provides a standalone browser layout and letter-sized
  print layout with a title page, two-column contents, portable Courier New
  code text, wrapped code blocks, tables, MathML, internal destinations, and
  print-safe page-break rules.
- The 2026-07-18 build used Pandoc 3.10 and Headless Chrome 150. It produced a
  2,315,561-byte self-contained HTML file and a 10,850,560-byte, tagged,
  unencrypted, 429-page PDF 1.4 with letter pages and a document outline.
- Validation: the full builder exited zero; Pandoc reported no math warnings;
  the HTML contains 410 MathML nodes; `pdfinfo` and `pdftotext` parsed the PDF;
  `pdftoppm` raster checks covered the title, system map, symbolic math,
  refinement, Cover proof layer, workflow bug tables, and late Cover parser;
  `audit-handbook.ps1 -Mode enforce` remained 4,063 / 4,063.
- Rebuild after every reader-facing handbook edit with
  `.\tools\docs\build-handbook.ps1`. Generated HTML, PDF, extracted text, and
  raster-validation files stay below ignored `build/` and are not source.

## Resume Procedure

1. Read `AGENTS.md`, this file, and `build/doc-audit/coverage.md`.
2. Confirm `git rev-parse HEAD` and run the audit with `-SyncLedger` if source
   signatures or files changed.
3. Work only on the current gate; keep each gate to a coherent algorithm or one
   to three closely related files.
4. Update ledger classifications and statuses only after the authored entry
   satisfies the completion contract.
5. Record exact next symbol, unresolved questions, and validation before ending.

## Evidence And Decisions

- The old audit reported 100 percent because it counted generated Ctags markers.
  That metric is retired.
- The corrected audit includes private/file-scoped helpers and currently
  inventories 4,063 production symbols.
- The raw generated appendix occupied most of the book and is being removed.
- The user wants mathematical/backend depth and only brief UI wiring coverage.
- Source-derived research effects are explained; expert intent that source
  cannot establish is labeled `[UNRESOLVED]` rather than invented.
- Documentation-only work is in scope. New tests and production trace hooks are
  deferred.
- The 2026-07-17 continuation added Sections 14.28-14.32 as completed work;
  Sections 14.33-14.40 are provisional reconnaissance only. BUG-052 through
  BUG-072 plus OPT-011 through OPT-016 remain source-evidenced agenda items.
  The high-priority defects include
  triple stable-polygon omission, concurrent cover artifact publication,
  MRR stable proof geometry reconstructed from double midpoints, duplicate
  diagnostic aggregation returning only the last triple, and SQLite's unbounded
  `SQLITE_BUSY` hot-spin retry. Do not relabel these findings when their source
  boundaries recur.
- No production or test source was changed in this handbook pass. The native
  wrapper rewrite added BUG-073 (partial allocation leak in
  `calculate_gradient`) to the agenda; the Java/JNA pass added BUG-074
  (ALL-info error erasure), BUG-075 (gradient return ABI mismatch), and
  BUG-076 (dropped invalid Vary rows). It did not silently fix any of them.

## Next-Section Reconnaissance Already Completed

- `wrapper.cpp` is the native ABI boundary immediately after the documented
  data layer. Its first function is `to_cstr` at line 49; later functions catch
  exceptions, preserve `backend_last_error`, construct WAL/full-sync pools, and
  expose MRR, cover, search, and information operations to Java.
- `sqlite_error_logging` relies on a one-time-before-open SQLite requirement.
  The wrapper's boot order and Java lifecycle must be documented before treating
  repeated application startup or pool recreation as safe.
- Preserve the distinction between a Java-visible native error, an ordinary
  mathematical non-result, a user cancellation, a SQLite busy wait, and a
  successful proof whose shared artifact publication failed.

## Unresolved

- Expert reasons for choosing one research parameter over another cannot always
  be recovered from code. Formal parameter effects can and must still be shown.
- Each source TODO or apparently unreachable refinement case must be explained
  as implemented before deciding whether it is a bug.
