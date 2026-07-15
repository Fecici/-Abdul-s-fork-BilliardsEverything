# Concurrency And MRR Fixes

Date: 2026-07-15

Scope: Abdul Windows fork only. This addendum records the concurrency-related bugs reported in `docs/nextIter.txt`, the source changes, retained parallelism, and validation.

## What The MRR Bug Was

For long stable codes with more than 1000 generated curves, `calculate_final_polygon` split the curve constraints among workers. Every worker started from the same rational bounding polygon, refined it with only its assigned curves, and returned a partial polygon. The caller then intersected those polygons.

That merge preserved geometric intersection but did not preserve the MRR boundary representation. Each valid final edge must retain the nonlinear sine/cosine equation and `LeftRight` witness that produced it. A batch intersection could keep an original linear bounding edge instead. In the reported CS, that edge was `-x-y+2eta`.

`stable_equations_to_string` passed that edge to `RearrangeVariant`. The linear-equation overload is an invariant guard and threw:

```text
this isn't supposed to happen-x-y+2eta
```

The program then made the problem worse: native `load_picture` returned `-1`, Java converted `-1` to `Optional.empty`, and `Database` cached that optional. The same code was therefore treated as a mathematical empty set for the remainder of the JVM session even though the backend had failed internally.

This was not primarily a data race. Parallelization changed which boundary metadata survived a geometrically valid intersection, violating a downstream representation invariant.

The Nick Shan Linux source contains the same batch-polygon reduction, so its presence there is not evidence that the representation is valid for every long input. Abdul keeps the useful Linux-style independent parallel stages but does not keep this defective dependent merge.

## Correct Parallelization Boundary

The curve-refinement sequence is a dependent reduction because each new polygon and its edge provenance feed the next constraint. That sequence now runs in deterministic set order and applies every constraint. The former `polygon_is_tiny` display-resolution exit was also removed because screen size is not a valid reason to stop a certified mathematical reduction.

MRR is still parallel in independent work:

- `Unfolding::generate_curves_lr` divides vertex-pair work among worker-local maps and merges them deterministically.
- Bounding-inequality construction retains its worker-local parallel loops.
- `refine_polygon` uses TBB to classify all current polygon corners for a curve in parallel, then performs the ordered topology update.
- `--threads=N` now installs a process-lifetime `tbb::global_control`, so TBB follows the same worker cap as the Boost pools.
- Java uses one fair interruptible outer MRR lock. One admitted MRR can use up to `N` internal workers; several AutoVary storage jobs cannot each create another `N`-worker native calculation at the same time.

This keeps useful MRR parallelism without parallelizing the provenance-sensitive reduction.

## AutoPolyVary Bug

When a code from the previous coordinate covered the current coordinate, `drawAutoPolyVary` recursively started the next coordinate but did not return. The old stack frame continued and created a `PolyVaryTask` for the already-skipped coordinate. Every occurrence forked another asynchronous callback chain sharing progress, executors, on-screen storage, cover text, and cancellation.

That explains the reported behavior:

- cycling around coordinates 170-173;
- progress reaching 4444 before the real traversal ended;
- repeated code output and duplicate cover entries;
- several callbacks printing the cancellation banner.

Covered coordinates now advance iteratively in the current frame. `AutoPolyVaryRun` owns the per-run resources and uses an atomic terminal claim, so only one callback can render the final state, shut down executors, close progress, or advance/cancel SuperPolyVary. Partial and final results share one processing path, and exact cover/small-cover lines are checked before append.

## AutoPolyVary Counter Race Follow-Up

The single-controller fix exposed a second, independent ordering bug. `setOBO` synchronously moved `PixelRadianMap` to the next coordinate, but the full region-image redraw was coalesced on the viewer executor. AutoPolyVary immediately called `autoRecurse` and read `regionsImageView`, so candidate discovery combined the new map transform with pixels from the previous coordinate.

After the first real calculation, this mismatch could produce zero uncovered points. The empty `PolyVaryTask` succeeded immediately, incremented the line counter, and moved to another coordinate before the pending redraw committed. The next redraw cancelled the previous coalesced redraw, making the zero-work cycle self-sustaining through line 4444. This is why the UI counter moved while no new vary calls or terminal results appeared.

AutoPolyVary now treats the coordinate redraw as an asynchronous prerequisite. The controller moves the map and requests its redraw, but creates or skips the coordinate task only from the callback that commits the matching JavaFX images. Rendering remains off the JavaFX thread. Each coordinate now prints its point number and uncovered candidate count; a legitimately covered view reports that it has no coordinates to vary instead of silently running an empty task.

## Native Failure Contract

MRR wrapper return values are now handled as:

- `1`: valid nonempty result;
- `0`: certified empty result, eligible for `Optional.empty` caching;
- `-1`: internal/native failure, raised as a Java exception with `backend_last_error` text.

This prevents an internal error from becoming persistent false mathematical data.

## Files Changed

- `src/backend/cpp/equations.cpp`: ordered exact reduction; removed unsafe batch merge and tiny-polygon exit.
- `src/backend/headers/utils.hpp`: process-lifetime TBB worker cap.
- `src/backend/cpp/wrapper.cpp`: preserve native MRR diagnostics and return `-1` for internal left/right failures.
- `src/java/billiards/wrapper/Wrapper.java`: fair outer MRR admission and strict `-1` failure handling.
- `src/java/billiards/database/Database.java`: cache only certified empty/nonempty results; wrapper owns admission.
- `src/java/billiards/viewer/Viewer.java`: single AutoPolyVary controller, redraw-before-candidate ordering, one terminal callback, shared result handling.
- `src/java/billiards/viewer/SmallCoverWindow.java`: exact line lookup for deduplication.
- `src/test/**` and `build.gradle`: real fixtures, worker determinism/cap checks, AutoPolyVary direction tests, and opt-in exact long regression.

## Validation

Commands run from the Abdul fork on 2026-07-15:

```powershell
.\gradlew.bat --no-daemon compileJava backendSharedLibrary
.\gradlew.bat --no-daemon testBackend test
.\gradlew.bat --no-daemon testBackendSlow
```

Results:

- Java/native integration build passed.
- Java tests passed, including the coordinate redraw/commit ordering regression.
- Fast native suite passed 33 cases.
- The exact reported 124-number CS produced a nonempty MRR with one worker and four workers.
- The one-worker and four-worker initial angles, equations, left/right witnesses, and interval point bounds matched exactly.

## Manual Release Checks

1. Launch with `--threads=2` and observe that long MRR work stays within the requested native budget.
2. Recalculate the reported long CS through the UI and confirm there is no invariant exception or `//empty set` warning.
3. Repeat the AutoPolyVary workflow around coordinates 170-173 through the configured end.
4. Cancel once and confirm one terminal banner, preserved completed results, and no repeated exact cover lines.
5. Run the reported workflow through line 4444 and confirm every non-covered point prints a positive uncovered-coordinate count before vary starts; the counter must not outrun its redraw and calculations.

## Deferred `nextIter.txt` Items

This pass intentionally did not implement the later requests for a structured backend logging system, incremental Vary terminal streaming, JVM-argument display changes, or further investigation of parked CPUs during Li-pattern OSNO work. Those remain future work; only the MRR/worker-budget and AutoPolyVary concurrency failures were changed here.
