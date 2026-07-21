# Calculate Add-To-Cover Result Reuse

Date: 2026-07-20

## Report

The top Calculate workflow reported the following formatted code as illegal when
`Add to Cover` was selected, although the LiPattern workflow accepted it:

```text
OSNO (108, 1168) 1 5 22 4 22 4 ... 12 4 30 6 31
```

The full 108-number input is retained in
`ClassifiedCodeSequenceTest.testReportedLongOsnoCalculateInput`. The regression
test passes the same formatted text through `Utils.tripleTrimmer`,
`Utils.splitString`, and `ClassifiedCodeSequence.create`. It verifies length
108, sum 1168, and type `OSNO` without opening or changing a database.

## Cause And Repair

The code sequence itself is legal. The top input field nevertheless had a
bidirectional converter attached to a temporary `SimpleObjectProperty`. The
converter created replacement arrays, but the Viewer continued using its
original `currentCodeNumbers` field. It was therefore a disconnected second
representation of the input rather than useful synchronization. Calculate now
owns the parse explicitly, while the second code window remains bound to the
same text field as before.

The handler also had a path difference that LiPattern did not: after a
successful calculation, `appendCalculatedCodeToCoverIfRequested` independently
classified the mutable current-input lists and called `Database.loadStorage` a
second time. Therefore calculation and Cover publication did not share one
result and could diverge.

The handler now carries each calculation's exact `Optional<Storage>` result
into Add to Cover. Publication performs no second classification and no second
database/native load. Stable singles and complete stable-unstable-stable
triples retain the existing admission and duplicate-line rules. An invalid,
empty, or incomplete calculation still adds nothing.

This is a workflow fix, not a change to OSNO legality or database contents.

## Validation

- The regression test is database-free and does not delete or insert the
  reported record.
- `gradlew --no-daemon compileJava backendSharedLibrary test` passed on
  2026-07-20. The native library tasks were up-to-date; Java production and
  test sources compiled, and the full Java test task passed.
- The regression was then run explicitly with `--tests
  billiards.codeseq.ClassifiedCodeSequenceTest.testReportedLongOsnoCalculateInput`;
  JUnit discovered and passed one test with no failures or errors.
