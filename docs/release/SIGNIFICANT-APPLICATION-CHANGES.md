---
title: "Significant Changes to BilliardsEverything"
subtitle: "Technical implementation report"
date: "19 July 2026"
---

# Purpose And Scope

This report explains the most significant behavioral and algorithmic changes
made to BilliardsEverything. It concentrates on the code that affects
calculation correctness, concurrency, memory ownership, research workflows,
and runtime performance. Toolchain configuration and installation details are
intentionally outside its scope.

The report is organized around the program's execution pipelines rather than a
chronological list of edits. Each section answers five questions:

1. What part of the application is involved?
2. What was wrong or unnecessarily expensive?
3. What implementation now exists?
4. Why does that implementation preserve the mathematical or lifecycle
   contract?
5. Which source files contain the change?

Several improvements originated in the Linux version of the application. They
were not copied as one undifferentiated patch. Some were incorporated directly,
some were adapted to the existing concurrency and ownership model, and some
were rejected because they violated an invariant or would have changed user
data location. Section 2 gives the complete crosswalk.

# 1. Execution Pipelines Affected By The Changes

The application contains several pipelines that share the same native
mathematical core. Understanding where a change sits is necessary to understand
its effect.

## Direct Calculation Pipeline

A direct calculation follows this path:

```text
typed code numbers
    -> Java validation and canonicalization
    -> native CodeSequence classification
    -> billiard unfolding and shooting vector
    -> sine/cosine boundary-curve generation
    -> rational bounding region
    -> interval polygon or line-segment refinement
    -> stable/unstable proof record
    -> database serialization
    -> Java Storage conversion
    -> screen rendering
```

The important fact is that the native result is richer than a polygon. A stable
MRR contains interval vertices, the equation associated with every boundary
edge, and the `LeftRight` witness describing which unfolding branches produced
that equation. A geometrically identical polygon with different edge metadata
is not an equivalent proof record.

## Vary Pipeline

VaryCS, Vary3, and Vary4 search a combinatorial tree of possible codes around a
coordinate or range of parameters:

```text
coordinate and search limits
    -> native depth-first generation of candidate words
    -> parallel candidate classification
    -> canonical code strings
    -> database/MRR calculation for selected codes
    -> partial Java results
    -> screen and optional Cover updates
```

There are two levels of concurrency here. Native Vary generates and classifies
candidate codes. Java then schedules storage calculations for accepted codes.
The changes bound both levels so a large search cannot create an unlimited
queue of expensive GMP/MPFR work.

## AutoPolyVary Pipeline

AutoPolyVary adds another controller above Vary:

```text
OBO coordinate
    -> update coordinate map
    -> render the matching view
    -> wait until that image is committed
    -> locate uncovered pixels
    -> run one PolyVaryTask
    -> publish partial regions
    -> advance exactly once
```

The render-commit step is a correctness boundary. The map and image must
describe the same coordinate before pixel colors are interpreted.

## Cover Pipeline

Cover checks whether known stable regions and triples cover a polygon or
rectangle:

```text
cover artifacts and target region
    -> parse stable/triple tables
    -> recursively subdivide squares
    -> interval-evaluate boundary equations
    -> mark filled and unfilled leaves
    -> write cover output and representative hole centers
```

This pipeline is sensitive to allocation cost because the same MPFR/MPFI
scratch objects were historically rebuilt for many recursive squares.

# 2. Linux-Derived Improvements And How They Were Integrated

The Linux version supplied a useful catalogue of performance and correctness
changes. The current implementation incorporates the high-value changes while
retaining the application-specific constraints that were already present.

## Incorporated Directly Or With Small Adaptations

| Change | Current implementation | Principal files |
| --- | --- | --- |
| Load uncovered cover squares | Writes representative centers to `tmp/holes.txt` and loads them through OBO navigation | `cover/save.cpp`, `Viewer.java` |
| Parallel corner classification | Uses `tbb::parallel_for` over a pre-sized corner vector | `refine.cpp`, `refine.hpp` |
| Linear-time code canonicalization | Uses least-rotation passes for forward and reversed words | `code_sequence.cpp`, `code_sequence.hpp` |
| Cached Vary pixel reader | Snapshots one `PixelReader` and image dimensions per task | `PolyVaryTask.java`, `CycleVaryTask.java` |
| Deterministic Vary traversal | Removes coordinate shuffling | `PolyVaryTask.java`, `CycleVaryTask.java` |
| Row-batched rendering | Submits one color task per image row instead of per pixel | `Viewer.java` |
| Pattern output construction | Replaces repeated `String +=` with `StringBuilder` | `PatternFinder.java`, `PatUtils.java` |
| Thread-local numeric evaluator | Reuses MPFR/MPFI scratch state per thread and precision | `evaluator.cpp/.hpp`, `common.cpp`, `verify.cpp` |
| Faster Vary serialization | Reserves buffers and avoids repeated output-string copying | `wrapper.cpp` |
| Linear cover annotation | Pre-indexes pattern suffixes in a map | `CoverWindow.java` |
| Short database ownership | Releases pooled SQLite handles during expensive geometry | `wrapper.cpp` |
| Native/Java layout correction | Matches `CInfoAll` field order and frees every native string | `CInfoAll.java`, `wrapper.cpp`, `Wrapper.java` |
| Numeric tolerance repair | Uses a practical `1e-25` Newton/intersection scale | `newton.hpp`, `intersection.hpp` |
| Vary4 right-list correction | Indexes `R[i]` while iterating the right branch | `triangle_billiard4.cpp` |
| Stale cover guards | Validates stable/triple indexes before dereferencing them | `CoverStuff.java` |
| Cover work off the UI thread | Runs expensive cover operations through JavaFX tasks | `CoverWindow.java` |

## Incorporated Differently

### Polygon Refinement

The Linux implementation tried to parallelize the entire curve-refinement
reduction by refining several copies of the bounding polygon and intersecting
the resulting polygons. The current implementation keeps the useful internal
parallel stages but performs boundary-producing refinements in deterministic
order. Section 3 explains why the outer reduction cannot be merged using only
polygon geometry.

### Render Coalescing

The single-flight render idea was retained, but only for the long-lived viewer
executor. Short-lived AutoVary and cover drawing executors still complete their
render synchronously because their owners shut those executors down immediately
after drawing. Coalescing is also disabled for a one-worker configuration so a
task cannot submit work back into the only thread and wait on itself.

### Vary Result Ownership

The native cleanup improvements were retained, but Java uses a fair outer Vary
lock instead of allowing several JNA calls to populate one synchronized result
list. The backend still has one process-wide cancellation flag. Serial outer
admission prevents one Vary operation from cancelling or resetting another
while each admitted call remains parallel internally.

### Cover Return Strings

Rather than returning references to static or merely thread-local strings, the
current JNA contract allocates caller-owned `CString` data and requires Java to
call `cleanup_string` in `finally`. This makes ownership explicit across the
language boundary.

### Reflection

The existing single `Affine` reflection transform was retained. It avoids
stacking duplicate transforms and uses the image side length rather than the
layout container height.

## Deliberately Not Incorporated

| Proposed change | Reason it was not retained |
| --- | --- |
| Independent MRR batch polygons followed by polygon intersection | It can preserve the geometric set while losing the equation and `LeftRight` provenance of final edges. |
| Stop refinement when a polygon becomes visually tiny | Display size is not a proof of mathematical emptiness and must not terminate a certification path. |
| Move the database directory | Existing databases are user data; changing the location without discovery and migration can make them appear lost. |
| Unconditional asynchronous render coalescing | It can deadlock a one-worker executor or let a short-lived owner shut down before its final image is committed. |
| Unbounded nested parallelism | Java MRR admission and native worker control prevent outer requests from multiplying the same CPU budget. |

# 3. MRR Calculation And The Safe Concurrency Boundary

## What An MRR Result Contains

`calculate_final_polygon` begins with a rational bounding polygon and refines it
against all sine and cosine inequalities generated from an unfolding. Each
element of the interval polygon is conceptually:

```text
(vertex interval, equation for the edge from this vertex to the next vertex)
```

The equation is later associated with `LeftRight` data. Serialization and proof
work therefore depend on both polygon topology and boundary origin.

## The Invalid Parallel Reduction

The removed approach partitioned curves into batches:

```text
P1 = refine(originalBoundingPolygon, batch1)
P2 = refine(originalBoundingPolygon, batch2)
P3 = refine(originalBoundingPolygon, batch3)
result = intersect(P1, P2, P3)
```

As sets of points, this resembles

$$
B \cap \bigcap_i H_i,
$$

where `B` is the initial bound and each `H_i` is a curve inequality. The problem
is that the program does not store an abstract set. If the merge keeps an edge
from `B`, that edge can still have a linear expression such as
`-x-y+2eta` even though a nonlinear trigonometric curve is the real active
boundary. Later code expects every final MRR edge to be rearrangeable as a
sine/cosine equation and fails on the retained bounding line.

This is not a data race. Every worker may be perfectly isolated and the result
can still be invalid because the reduction operator discards state.

## Ordered Boundary-Producing Reduction

The current loop carries the complete polygon-with-equations value from one
curve to the next:

```cpp
for (const auto& kv : curves.first) {
    const auto maybe = refine_polygon(interval_polygon, kv.first);
    if (!maybe) {
        return boost::none;
    }
    interval_polygon = std::move(*maybe);
}

for (const auto& kv : curves.second) {
    const auto maybe = refine_polygon(interval_polygon, kv.first);
    if (!maybe) {
        return boost::none;
    }
    interval_polygon = std::move(*maybe);
}
```

The move assignment avoids copying a completed polygon, but it does not change
the order or the boundary metadata.

## Parallel Curve And Inequality Generation

Unfolding and bounding-inequality generation remain parallel because their
individual outputs are independent before deterministic set merging. Work is
chunked to approximately the configured worker count, and each task writes to
its own container:

```cpp
const unsigned int concurrency = billiards_worker_count();
const std::size_t task_num = billiards_task_count(left_n, concurrency);
const std::size_t block_size = billiards_block_size(left_n, task_num);

std::vector<Curves> thread_curves(task_num);
boost::asio::thread_pool pool(concurrency);

for (std::size_t t = 0; t < task_num; ++t) {
    const size_t begin = t * block_size;
    const size_t end = std::min(begin + block_size, left_n);
    boost::asio::post(pool, [&, begin, end, t] {
        // Generate into thread_curves[t].
    });
}
pool.join();
```

This avoids concurrent mutation of a shared `std::set`. It also prevents the
number of local result containers from growing with the number of equations.

`bounding_inequalities.cpp` uses the same pattern. Its phi-elimination step
buffers results per task, periodically sorts and deduplicates large buffers,
then merges them into the final set. This bounds duplicate-heavy intermediate
storage.

## Parallel Corner Classification

For one current polygon and one curve, the sign at each current vertex can be
calculated independently. The result vector is sized before the parallel loop,
so worker `i` writes only `corners[i]`:

```cpp
std::vector<Corner> corners(size);
tbb::parallel_for(tbb::blocked_range<size_t>(0, size),
    [&](const tbb::blocked_range<size_t>& range) {
        for (size_t i = range.begin(); i != range.end(); ++i) {
            const auto curve_sign =
                curve_sign_at_point(curve, polygon.at(i).point);
            if (curve_sign == Sign::NEG) {
                corners[i] = Corner(Sign::NEG);
            } else if (curve_sign == Sign::POS) {
                corners[i] = Corner(Sign::POS);
            } else {
                // The ZERO branch also evaluates the adjacent edge and curve
                // gradients before constructing Corner(Sign::ZERO, ZeroInfo).
            }
        }
    });

correct_zeros(corners);
```

`correct_zeros` and the topology-changing case analysis execute after the join.
This is the correct division: point classification is parallel; polygon
mutation is ordered.

## Bounding-Box Sign Short-Circuit

Before classifying every corner, `refine_polygon` interval-evaluates the curve
over a rectangle enclosing the current polygon:

```cpp
const Vector2<Interval> bb_point{
    Interval{x_low, x_high}, Interval{y_low, y_high}
};
const auto bb_sign = curve_sign_at_point(curve, bb_point);

if (bb_sign == Sign::POS) {
    return polygon;
}
if (bb_sign == Sign::NEG) {
    return boost::none;
}
```

An interval-positive result proves that the entire bounding box satisfies the
inequality, so returning the polygon is safe. An interval-negative result proves
that none of the polygon can survive. If the interval contains zero, the code
falls back to full corner classification. This is a proof-preserving
short-circuit, unlike the removed visual-size cutoff.

## One Outer MRR Request At A Time

Java uses a fair interruptible lock around each native MRR entry:

```java
private static final ReentrantLock NATIVE_MRR_LOCK =
        new ReentrantLock(true);

private static void beginNativeMrr(final String operationName) {
    try {
        NATIVE_MRR_LOCK.lockInterruptibly();
    } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
                "Interrupted while waiting to start " + operationName, e);
    }
}
```

This does not make the calculation single-threaded. It prevents several Java
requests from each creating their own native parallel workload at the same
time. The admitted request still uses parallel unfolding, bounding work, and
corner classification.

## Regression Contract

The MRR tests compare:

- lower and upper endpoints of every interval vertex;
- boundary equations;
- `LeftRight` witnesses;
- results produced under different worker counts.

Comparing only median points would not detect the original provenance defect.

Principal files:

- `src/backend/cpp/equations.cpp`
- `src/backend/cpp/unfolding.cpp`
- `src/backend/cpp/bounding_inequalities.cpp`
- `src/backend/cpp/refine.cpp`
- `src/backend/headers/refine.hpp`
- `src/backend/headers/utils.hpp`
- `src/java/billiards/wrapper/Wrapper.java`
- `src/test/headers/equations_test.hpp`

# 4. Native Vary Parallelism And Memory Bounds

## Search Structure

VaryCS and Vary3 walk a depth-first search using an explicit `Frame` stack.
Candidate generation is sequential because each frame mutates the current code,
side sum, depth, and billiard state. When a candidate reaches a classification
point, verification can run independently in the native pool.

Vary4 creates independent starting states by splitting the search tree to a
depth sufficient to feed the configured workers. Each pool task owns its code,
side sum, billiard state, and local result vector.

## Bounded In-Flight Verification

Candidate verification can allocate GMP/MPFR state outside the Java heap. A
fast producer must not enqueue thousands of expensive classifications before
workers catch up. VaryCS and Vary3 combine a memory estimate with a worker-based
cap:

```cpp
const unsigned int cores = billiards_worker_count();
const int memoryInflight =
        std::max(1, compute_max_inflight(usage, 16384));
const int MAX_INFLIGHT =
        std::max(1, std::min(memoryInflight,
                             static_cast<int>(cores * 4)));
```

The usage fraction becomes smaller for very large requested depths. The
`cores * 4` bound prevents a high-memory machine estimate from creating an
enormous queue anyway.

Vary4 uses one in-flight task per worker:

```cpp
const int MAX_INFLIGHT = static_cast<int>(cores);
while (inflight >= MAX_INFLIGHT) {
    std::this_thread::sleep_for(std::chrono::microseconds(100));
}
```

The loop is simple backpressure. It trades a small producer wait for bounded
queued state.

## Worker-Local Results And Failure Publication

Each native task accumulates into a local vector. It takes a mutex only while
appending completed results to the shared output. Worker exceptions are caught,
and the first message is stored under a separate mutex. After the pool joins,
the calling thread can fail the JNA operation coherently instead of allowing an
exception to terminate a worker silently.

The explicit stacks reserve capacity (`stack.reserve(max * 2)` in VaryCS), and
candidate vectors are moved into later stages where ownership transfers. These
changes reduce allocator traffic without changing search order.

## Why Outer Vary Admission Is Serialized

The native `cancel_flag()` is process-wide:

```cpp
inline std::atomic<bool>& cancel_flag() {
    static std::atomic<bool> f{false};
    return f;
}
```

Each Vary operation resets and reads that same flag. Two concurrent JNA Vary
calls could cancel or re-enable one another. Java therefore uses a fair
`NATIVE_VARY_LOCK`. One outer call owns the global cancellation state, while its
native candidate verification remains parallel.

Principal files:

- `src/backend/cpp/vary_cs.cpp`
- `src/backend/cpp/vary3.cpp`
- `src/backend/cpp/vary4.cpp`
- `src/backend/cpp/triangle_billiard4.cpp`
- `src/backend/headers/utils.hpp`
- `src/java/billiards/wrapper/Wrapper.java`

# 5. AutoPolyVary Controller, Counter, And Render Ordering

## Defect One: More Than One Owner

One branch detected that the current coordinate was already covered by a code
from the preceding coordinate. It started the next coordinate but did not
return. The old branch could therefore continue and create work for the current
coordinate as well. Repeated occurrences produced multiple callback chains
sharing executors, progress state, and the coordinate counter.

Consequences included:

- repeated coordinates;
- a counter that advanced faster than completed calculations;
- premature progress closure;
- multiple completion or cancellation banners;
- executor shutdown while another chain still expected to submit work.

The covered-coordinate branch now advances and immediately returns.

## Defect Two: New Map With Old Pixels

Changing OBO position updates the coordinate map immediately. The image redraw
is asynchronous. If candidate discovery runs between those events, it converts
coordinates to pixels with the new map but samples the previous image. That can
make a real uncovered area look covered, produce a zero-work task, and advance
the line counter almost instantly.

The required ordering is:

```text
map N selected
    -> image N calculated
    -> image N committed on JavaFX
    -> pixels for N inspected
    -> coordinate N completed
    -> counter advances to N+1
```

## Per-Run State Object

`AutoPolyVaryRun` owns all state with the same lifetime:

```java
private final class AutoPolyVaryRun {
    private final ExecutorService drawExecutor;
    private final ExecutorService storageExecutor;
    private final ExecutorService shotExecutor;
    private final ProgressMultiTask progress;
    private final Optional<SimpleObjectProperty<Integer>> step;
    private final AtomicBoolean terminal = new AtomicBoolean(false);
    // run options omitted
}
```

This replaces implicit ownership spread across recursive methods and local
callbacks.

## Exactly One Terminal Transition

Success, cancellation, and failure callbacks can race after queued work has
already been scheduled. Each terminal path must claim the run once:

```java
private void finish(final boolean cancelled) {
    if (!terminal.compareAndSet(false, true)) {
        return;
    }

    renderRegions(onScreenSequences, guideLinesImageView,
                  regionsImageView, drawExecutor);
    Utils.shutdownExecutorAsync(storageExecutor);
    Utils.shutdownExecutorAsync(shotExecutor);
    // publish one success or cancellation result
    progress.close();
}
```

Late callbacks see `terminal == true` and return without rendering, closing
resources, or advancing an enclosing SuperPolyVary step.

## Render Commit As A Continuation

Coordinate work is passed to the renderer as an `afterCommit` continuation:

```java
lineNumberTxt.setText(Integer.toString(currIdx + 1));
startAutoPolyVaryCoordinateAfterRender(
    afterCommit -> setOBOAfterRender(
        currIdx, run.drawExecutor, afterCommit,
        failure -> {
            run.fail();
            throw failure;
        }),
    () -> continueAutoPolyVaryCoordinate(
        maxSubdivisions, currIdx, endIdx, stepIdx,
        area, colorOpt, run, previousCodes));
```

`setOBOAfterRender` may calculate images in the background, but it invokes the
continuation only after the matching images are installed on the JavaFX thread.

## Counter Invariant

The controller now treats a coordinate as complete only after one of these
events:

- a previous stable region is proved to contain it;
- its committed image contains no uncovered candidate pixels;
- its one `PolyVaryTask` reaches a terminal callback;
- cancellation or failure claims the run.

`hasNextAutoPolyVaryIndex` handles forward and reverse ranges explicitly and
rejects a zero step. Empty uncovered work prints an explicit message and uses
the normal single advance path; it no longer creates another controller.

Principal files:

- `src/java/billiards/viewer/Viewer.java`
- `src/java/billiards/viewer/SmallCoverWindow.java`
- `src/java/billiards/viewer/PolyVaryTask.java`
- `src/test/java/billiards/viewer/ViewerAutoPolyVaryTest.java`

# 6. Rendering Pipeline And Stale-Frame Prevention

## Row Batching

A normal redraw previously created approximately `SIDE * SIDE` futures and then
waited for them one pixel at a time. For a 1000 by 1000 image, scheduler and
future overhead could dominate the actual color calculation.

The renderer now submits `SIDE` row jobs:

```java
final Future<Color[]>[] rowFutures = new Future[SIDE];
for (int pixelX = 0; pixelX < SIDE; ++pixelX) {
    final double rx = rxs[pixelX];
    rowFutures[pixelX] = executor.submit(() -> {
        final Color[] row = new Color[SIDE];
        for (int pixelY = 0; pixelY < SIDE; ++pixelY) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Render row cancelled");
            }
            final double ry = rys[pixelY];
            row[pixelY] = color(stableRegions, regions, rx, ry,
                                    halfWidth, offset,
                                    options.proverSelected);
        }
        return row;
    });
}
```

The parent collects complete rows, writes the final image, and cancels remaining
row futures if interrupted or failed. The output is the same grid; only the
task granularity changed.

## Snapshotting UI Options

Checkbox state is copied into a small `RenderOptions` value before background
work begins. Pixel loops no longer read JavaFX controls from worker threads and
one frame cannot mix option values changed halfway through rendering.

## Generation-Based Coalescing

Every long-lived viewer render receives a monotonically increasing generation:

```java
final long generation = renderGeneration.incrementAndGet();
final Future<?> previousRender = renderFuture;
if (previousRender != null) {
    previousRender.cancel(true);
}
```

The worker checks both interruption and generation before publication. The
JavaFX callback checks the generation again before installing the images. An
older render is therefore unable to overwrite a newer zoom, reflection, code,
or cover state even if cancellation arrives late.

The region map is copied before submission because UI callbacks may modify the
live `LinkedHashMap` during background drawing.

## Guarded Synchronous Path

Coalescing is used only when:

- the caller uses the persistent viewer executor;
- render coalescing is enabled;
- more than one worker is available.

Other callers build and commit their images in their existing task. The
continuation is posted with `Platform.runLater`, which avoids a deeply recursive
chain when many consecutive AutoPolyVary coordinates contain no work.

## Reflection Geometry

The image stack is pinned to `SIDE x SIDE`, and reflection uses one reusable
transform:

```java
reflectionTransform.setMyy(-1);
reflectionTransform.setTy(SIDE);
if (!imageStack.getTransforms().contains(reflectionTransform)) {
    imageStack.getTransforms().add(reflectionTransform);
}
```

Using the layout container height was incorrect because a `BorderPane` can
allocate more vertical space than the image. Reusing one transform also prevents
repeated toggles from stacking multiple reflections.

Principal files:

- `src/java/billiards/viewer/Viewer.java`
- `src/java/billiards/viewer/Utils.java`

# 7. Native/JNA Memory Ownership And Failure Semantics

## Exact Structure Layout

JNA calculates native field offsets from `getFieldOrder()`, not from the visual
order of Java declarations. `CInfoAll` now returns the same order as the C
structure:

```java
return FastList.newListWith(
    "initial_angles", "points", "equations",
    "sinEquations", "cosEquations",
    "left_rights", "code_seq_lr",
    "vectorX", "vectorY");
```

Without this correction, Java could interpret one `char*` as another field and
later free the wrong address.

## Ownership Rules

| Native result | Allocation owner | Required release |
| --- | --- | --- |
| `CPicture` strings | C++ `new[]` | `cleanup_cpicture` |
| `CInfo` strings | C++ `new[]` | `cleanup_cinfo` |
| `CInfoAll` strings | C++ `new[]` | `cleanup_cinfo_all` |
| `CString.string` | C++ `new[]` | `cleanup_string` |
| connection-pool pointer | C++ `new` | `destroy_connection_pool` |

Java conversions use `try/finally`, so parsing failures do not skip native
cleanup.

## Complete `CInfoAll` Cleanup

All nine fields are released:

```cpp
void cleanup_cinfo_all(const CInfoAll* const info) {
    delete[] info->initial_angles;
    delete[] info->points;
    delete[] info->equations;
    delete[] info->sinEquations;
    delete[] info->cosEquations;
    delete[] info->left_rights;
    delete[] info->code_seq_lr;
    delete[] info->vectorX;
    delete[] info->vectorY;
}
```

These allocations live outside the managed Java heap. Missing cleanup can grow
native memory during long sessions even when Java garbage collection appears
normal.

## Exception-Safe Publication

The native copy helpers allocate into temporary pointer variables. Only after
every conversion succeeds are the pointers assigned to the outgoing structure.
If a later allocation throws, the catch block deletes every earlier temporary
and rethrows. Java therefore receives either a complete structure or no
published ownership.

The pattern is:

```cpp
char* first = nullptr;
char* second = nullptr;
try {
    first = to_cstr(value1);
    second = to_cstr(value2);
} catch (...) {
    delete[] first;
    delete[] second;
    throw;
}
out->first = first;
out->second = second;
```

## Error Versus Empty Result

MRR-related native calls use a three-way status:

```text
-1 = program/backend failure
 0 = certified mathematical empty result
 1 = nonempty calculated result
```

A thread-local diagnostic stores the exception message:

```cpp
thread_local std::string backend_error_message;

catch (const std::runtime_error& except) {
    remember_backend_error("load_picture", except);
    return -1;
}
```

Java reads `backend_last_error()` and throws for `-1`. Only `0` becomes
`Optional.empty()`. This prevents an internal failure from being cached or
displayed as a mathematical empty set.

Principal files:

- `src/backend/cpp/wrapper.cpp`
- `src/backend/headers/wrapper.hpp`
- `src/java/billiards/wrapper/CInfoAll.java`
- `src/java/billiards/wrapper/Wrapper.java`

# 8. Database Connection Ownership And Storage Cache

## Short Connection Scopes

The old native picture path could borrow one SQLite connection and retain it
while calculating a stable or unstable region. Geometry can take seconds or
minutes, while the pool contains only a limited number of handles. Concurrent
UI and AutoVary requests could wait even though the long calculation was not
using SQLite.

The current sequence is:

```cpp
bool already_in_db = false;
{
    sqlite::PooledConnection conn{*pool};
    already_in_db = database::in(code_sequence, code_type, conn.db);
}

if (!already_in_db) {
    const auto result = calculate_stable(code_sequence, code_type);
    if (!result) return 0;

    sqlite::PooledConnection conn{*pool};
    database::save(code_sequence, code_type, *result, conn.db);
}

{
    sqlite::PooledConnection conn{*pool};
    const auto picture =
        database::load_picture(code_sequence, code_type, conn.db);
    copy_to_cpicture(picture, cpicture);
}
```

RAII returns each connection when its scope ends. The expensive calculation
runs between scopes without a database handle. The left/right Expando picture
paths follow the same ownership split.

## Bounded Storage Cache

Java keeps up to 1,000 `ClassifiedCodeSequence -> Optional<Storage>` entries in
a synchronized insertion-ordered map:

```java
private static final int CACHE_MAX_SIZE = 1000;
private static final Map<ClassifiedCodeSequence, Optional<Storage>>
        STORAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>() {
                protected boolean removeEldestEntry(final Map.Entry eldest) {
                    return size() > CACHE_MAX_SIZE;
                }
            });
```

Both nonempty values and certified empty results can be reused. Backend failures
are thrown and never inserted as empty values.

The current cache is not a single-flight calculation registry. Its second cache
check occurs immediately after the first, without waiting on an ownership token,
so two callers can still miss concurrently and calculate the same code. The
cache bounds retained results and accelerates later repeats; it does not prove
that duplicate in-flight work cannot happen.

## Diagnostic Heartbeat

`loadStorage` schedules a daemon heartbeat while a native call is active and
cancels it in `finally`. Printing is disabled by default, but the hook can be
enabled when diagnosing a calculation that appears stuck. Because the scheduler
thread is daemonized, it cannot keep the process alive at shutdown.

Principal files:

- `src/backend/cpp/wrapper.cpp`
- `src/java/billiards/database/Database.java`
- `src/java/billiards/wrapper/ConnectionPool.java`

# 9. Numerical And Core Algorithm Changes

## Newton And Intersection Tolerances

The previous Newton convergence and intersection widening values were around
`1e-40` and `1e-45`. At the working precision used by real long sequences,
requiring that scale can cause a valid iteration to be reported as
non-convergent or produce an interval too narrow for a reliable inside test.

Both paths now use `1e-25`:

```cpp
const Real eps{"1e-25"};
const Real fudge{"1e-25"};
```

The value remains far below displayed angular resolution. Its purpose is not
to make the proof approximate; it gives the interval/Newton machinery a
consistent attainable scale instead of demanding accuracy beyond the practical
working precision.

## Vary4 Right-Branch Indexing

`TriangleBilliard4::reconfigure(false)` iterates over the right candidate list.
The earlier branch nevertheless subtracted `L[i]`. If `R` and `L` have
different lengths, this is both wrong geometry and a possible out-of-bounds
read. The corrected operation is:

```cpp
for (size_t i = 0; i < R.size(); ++i) {
    Vector2D direc = /* ... */;
    direc.sub(R[i]);
    // ...
}
```

This affects Vary4 candidate construction before later code classification, so
an incorrect vector here can contaminate the entire returned set.

## Linear-Time Cyclic Canonicalization

A billiard code has no distinguished starting hit, and traversing the orbit in
reverse represents the same cyclic object. Canonicalization must choose the
lexicographically smallest member of

$$
\{\operatorname{rot}_k(c)\}_{k=0}^{n-1}
\cup
\{\operatorname{rot}_k(\operatorname{reverse}(c))\}_{k=0}^{n-1}.
$$

The old implementation materialized and compared every rotation, which is
quadratic. The new `least_rotation_index` scans a doubled word and skips ranges
of starting positions already known to be worse:

```cpp
while (i < n) {
    answer = i;
    size_t j = i + 1;
    size_t k = i;
    while (j < 2 * n && doubled.at(k) <= doubled.at(j)) {
        if (doubled.at(k) < doubled.at(j)) {
            k = i;
        } else {
            ++k;
        }
        ++j;
    }
    while (i <= k) {
        i += j - k;
    }
}
```

One pass finds the best forward rotation; a second pass handles the reversed
word. `rotated_copy` materializes only the two candidates that need comparison.
The mathematical equivalence relation is unchanged while asymptotic time falls
from `O(n^2)` to `O(n)`.

`CodeSequence::type()` is also cached. A valid `CodeSequence` is immutable, so
odd/even, closed/open, and stable/unstable classification does not need to be
recomputed at every consumer.

## Thread-Local MPFR/MPFI Evaluator

`Evaluator` owns mutable `mpfr_t` and `mpq_t` scratch registers. Sharing one
instance between threads would race; constructing a new one for each cover
square repeatedly initializes expensive multiple-precision objects.

The current compromise is one instance per thread and precision:

```cpp
Evaluator& Evaluator::thread_local_instance(const uint32_t prec) {
    thread_local std::unique_ptr<Evaluator> instance;
    thread_local uint32_t instance_prec = 0;

    if (!instance || instance_prec != prec) {
        instance.reset(new Evaluator(prec));
        instance_prec = prec;
    }
    return *instance;
}
```

Each TBB or Java-triggered native worker gets isolated mutable registers. The
instance is rebuilt only if a later operation asks that thread for a different
precision. A thread-local `FreeCache` calls `mpfr_free_cache()` when the thread
exits.

Principal files:

- `src/backend/headers/newton.hpp`
- `src/backend/headers/intersection.hpp`
- `src/backend/cpp/triangle_billiard4.cpp`
- `src/backend/cpp/code_sequence.cpp`
- `src/backend/headers/code_sequence.hpp`
- `src/backend/cpp/evaluator.cpp`
- `src/backend/headers/evaluator.hpp`
- `src/backend/cpp/common.cpp`
- `src/backend/cpp/verify.cpp`
- `src/test/headers/code_sequence_test.hpp`

# 10. AutoVary Pixel Sampling And Deterministic Traversal

`PolyVaryTask` and `CycleVaryTask` repeatedly ask whether a coordinate maps to
an already-filled pixel. The older helper created a `FutureTask`, posted it to
JavaFX, and blocked on `get()` for every coordinate. A search involving
thousands of points therefore performed thousands of UI-thread round trips.

## One Snapshot Per Task

At task start, the code obtains the image, `PixelReader`, width, and height on
the JavaFX thread. The worker then reads the stable snapshot directly:

```java
private int pixelColor(final Vector2 point) {
    final PixelReader reader = this.pixelReader;
    if (reader == null) {
        return 0;
    }

    final int midX = (int) this.screenMap.pixelX(point.x);
    final int midY = (int) this.screenMap.pixelY(point.y);
    if (midX < 0 || midY < 0
            || midX >= this.imgWidth || midY >= this.imgHeight) {
        return 0;
    }
    return reader.getArgb(midX, midY);
}
```

The explicit bound check handles coordinates just beyond an image edge after
zoom, reflection, or floating-point rounding. Returning the uncovered color
lets the normal calculation decide the point rather than aborting the complete
search with `IndexOutOfBoundsException`.

## Removed Shuffle

Coordinate lists are no longer passed through `Collections.shuffle`. Stable
ordering improves reproducibility and spatial locality and makes cancellation
and counter problems diagnosable from logs.

## Cancellation Semantics

Pixel-cache initialization restores interrupt status if it is interrupted.
Each task checks graceful cancellation before starting expensive work and while
draining futures. Queued storage work is cancelled, while already running work
may finish and publish useful partial results.

Principal files:

- `src/java/billiards/viewer/PolyVaryTask.java`
- `src/java/billiards/viewer/CycleVaryTask.java`
- `src/java/billiards/viewer/GracefullyCancelable.java`

# 11. Cover Reliability, Performance, And Workflow Features

## Defensive Cover Artifact Loading

`cover.txt` contains indexes into companion stable and triple tables. Old,
partial, or hand-edited folders can contain an index outside the available
list. Direct access previously crashed the viewer.

The parser now checks each reference:

```java
if (index >= 0 && index < stables.size()) {
    // use the stable
} else {
    System.err.println(
        "Warning: cover.txt references stable index " + index
        + " but stables.txt has only " + stables.size()
        + " entries. Skipping.");
}
```

Triples receive the same guard. A bad leaf is skipped with a diagnostic instead
of destroying the entire loaded cover.

This is index safety, not complete semantic validation. The parser still needs
future hardening to verify that stable and unstable code types occur in the
roles required by each artifact.

## Linear Cover Annotation

`CoverWindow.redoInfo` used to scan every pre-info line for every `info.txt`
line, giving `O(n*m)` string work. It now normalizes the pre-info codes once into
a `HashMap<String,String>` and performs one lookup per output row:

```java
final Map<String, String> preInfoMap = new HashMap<>();
for (final String preLine : preInfo.split("\\r?\\n")) {
    final String trimmed = Utils.tripleTrimmer(
        preLine.split("#")[0].trim());
    if (trimmed.isEmpty()) {
        continue;
    }
    if (preLine.contains("#")) {
        preInfoMap.put(trimmed, preLine.split("#")[1].trim());
    } else {
        preInfoMap.put(trimmed, "");
    }
}

for (final String line : info.split("\\r?\\n")) {
    String code = line;
    if (code.contains(" - ")) {
        code = code.startsWith("-")
                ? code.split(" - ")[2]
                : code.split(" - ")[1];
    }
    final String suffix = preInfoMap.get(code);
    if (suffix != null && !suffix.isEmpty()) {
        postInfo.append(" # ").append(suffix);
    }
}
```

The output ordering stays the same; only lookup complexity changes to `O(n+m)`.

## Load Holes

After cover subdivision, native code has a vector of rectangles that were not
filled. `cover::save_holes` writes representative centers in degrees:

```cpp
const size_t num_to_print = std::min(not_filled.size(), empties);
if (num_to_print != 0) {
    const size_t inc = not_filled.size() / num_to_print;
    for (size_t i = 0; i < num_to_print * inc; i += inc) {
        file << center_degrees(not_filled.at(i)) << '\n';
    }
}
```

When there are more holes than requested, the fixed stride samples across the
whole result instead of taking only the first cluster.

`Viewer.loadHolesFromFile`:

1. checks for `tmp/holes.txt`;
2. reads nonblank coordinate lines;
3. asks how many to load;
4. writes the selected coordinates to `tmp/holes_obo.txt`;
5. uses the ordinary OBO parser;
6. displays the first coordinate with the existing forward/back and line-number
   controls.

The feature converts cover output directly into the next research input:
uncovered square -> coordinate -> Vary/Calculate -> new code -> revised cover.

## Calculate: Add To Cover

The existing Calculate action has an optional `Add to Cover` checkbox. After
calculation, Java reloads the current code entries as typed `Storage` values and
accepts only valid Cover roles:

```java
if (storages.size() == 1) {
    final Storage storage = storages.get(0);
    if (storage.classCodeSeq.stable) {
        final String entry = getCoverCodeString(storage);
        if (!coverWindow.containsStableInfo(entry)) {
            coverWindow.appendStablesInfo(entry);
        }
    }
} else if (storages.size() == 3
        && storages.get(0).classCodeSeq.stable
        && !storages.get(1).classCodeSeq.stable
        && storages.get(2).classCodeSeq.stable) {
    final String entry = tripleString.toString();
    if (!coverWindow.containsTripleInfo(entry)) {
        coverWindow.appendTriplesInfo(entry);
    }
}
```

Unstable singles, empty calculations, invalid inputs, incomplete triples, and
wrong stable/unstable order are not appended. `CoverWindow.containsStableInfo`
and `containsTripleInfo` compare normalized complete lines so repeating the same
calculation does not create duplicate entries.

Principal files:

- `src/backend/cpp/cover/save.cpp`
- `src/backend/headers/cover/save.hpp`
- `src/backend/cpp/verify.cpp`
- `src/java/billiards/cover/CoverStuff.java`
- `src/java/billiards/viewer/CoverWindow.java`
- `src/java/billiards/viewer/Viewer.java`

# 12. Cancellation, Executor Lifetimes, And Window Closure

## Graceful Versus Immediate Cancellation

`GracefullyCancelable` distinguishes two intentions:

- stop discovering or accepting new work;
- preserve already completed and, where appropriate, already running partial
  results.

Progress windows call `requestGracefulCancel()` on tasks that support it. Vary
tasks stop their producer loop, cancel queued futures, drain acceptable running
work, and publish accumulated storage results.

## Shared Shutdown Helpers

`Utils.safeShutdownExecutor` follows the standard sequence:

1. call `shutdown()` so no new work is accepted;
2. wait for a bounded interval;
3. call `shutdownNow()` if tasks remain;
4. wait again;
5. restore interrupt status if the waiting thread is interrupted.

`shutdownExecutorAsync` runs that process from a daemon helper when blocking
the JavaFX thread would freeze the interface. Task-local executors are closed
from terminal handlers or `finally` blocks rather than only from the success
path.

`PolyVaryTask` treats a `RejectedExecutionException` during an already-requested
shutdown as cancellation rather than a new application failure. Rejection while
the executor is otherwise live remains an error.

## Main And Child Windows

Main-window shutdown explicitly saves and closes owned child windows. This is
necessary because programmatic `Stage.close()` does not necessarily run the
same close-request handler as a user click. Cover windows therefore save their
contents in their explicit `close()` methods.

`IterateToLimitWindow` had a separate null-lifecycle defect. AutoVary can create
it only as a result sink, without calling the method that initializes its
`finish` property. Main shutdown then called `finish.set(true)` on null. The
current guard is:

```java
static void markFinishedIfPresent(final SimpleBooleanProperty finish) {
    if (finish != null) {
        finish.set(true);
    }
}
```

Closing this optional window can no longer prevent the rest of the application
from shutting down cleanly.

Principal files:

- `src/java/billiards/viewer/GracefullyCancelable.java`
- `src/java/billiards/viewer/Utils.java`
- `src/java/billiards/viewer/Progress.java`
- `src/java/billiards/viewer/ProgressWithStatus.java`
- `src/java/billiards/viewer/ProgressMultiTask.java`
- `src/java/billiards/viewer/Main.java`
- `src/java/billiards/viewer/Viewer.java`
- `src/java/billiards/viewer/IterateToLimitWindow.java`
- `src/java/billiards/viewer/CoverWindow.java`
- the draw, Vary, AutoVary, and result-window classes under `viewer/`

# 13. Shared Worker Budget

The application accepts one worker-count choice and applies it to Java and
native work. Java resolves and clamps the requested value, creates its executors
from that number, and calls `backend_set_worker_threads`.

The native setter stores the count for Boost.Asio pools and installs a persistent
TBB `global_control`:

```cpp
tbb_worker_control().reset(new tbb::global_control{
    tbb::global_control::max_allowed_parallelism,
    static_cast<std::size_t>(worker_count)
});
configured_worker_count().store(worker_count,
                                std::memory_order_relaxed);
```

Consequences:

- unfolding and bounding pools use the chosen count;
- Vary pools and their queue caps use the chosen count;
- TBB corner classification observes the same maximum;
- Java render/storage/shot executors are derived from the same user-facing
  budget;
- outer MRR and outer Vary admission prevent nested requests from multiplying
  it uncontrollably.

The native environment override remains available for direct native tests that
do not start through Java.

Principal files:

- `src/java/billiards/viewer/Main.java`
- `src/java/billiards/viewer/Utils.java`
- `src/java/billiards/wrapper/Wrapper.java`
- `src/backend/headers/utils.hpp`
- `src/backend/cpp/wrapper.cpp`
- `src/backend/headers/wrapper.hpp`

# 14. Output Construction And Smaller Allocation Improvements

## Pattern Finder

Several pattern paths repeatedly appended to immutable Java strings. If output
length is `N`, repeated `result += fragment` can copy a growing prefix at each
step and approach `O(N^2)` character movement. `PatternFinder` and `PatUtils`
now retain mutable `StringBuilder` objects through their loops and call
`toString()` only at the boundary.

This applies to:

- input normalization;
- error-line summaries;
- single, triple, and extension result blocks;
- pattern headers and repeated-code expansion;
- stable/triple extraction for cover text.

The textual grammar is unchanged.

## Native Vary Serialization

Native Vary result formatting now reserves an estimated buffer and appends code
numbers into it before one JNA string allocation. This reduces repeated growth
and intermediate stream/string copies when a search returns many codes.

## Container Reservations And Moves

Other hot paths received local allocation changes:

- Vary DFS stacks reserve from the requested maximum depth;
- curve and inequality tasks keep one local result container per worker-sized
  chunk rather than per input element;
- refined polygons are moved into the next reduction step;
- candidate vectors are move-captured or moved into sorted stages when the old
  owner no longer needs them;
- result vectors are pre-sized when worker indexes write disjoint slots.

These edits do not change mathematical branching or output ordering. They
reduce copying and the size of temporarily live allocation graphs.

## Geometry Projection Interface

`ConvexPolygon`, `LineSegment`, and `Rectangle` share the `Projectable`
interface for separating-axis calculations:

```java
public interface Projectable {
    Interval project(Vector2 axis);
}
```

Coordinate-axis projections (`projectX`, `projectY`) avoid constructing generic
axes for common rectangle checks. Small geometry values also use direct hash
mixing rather than allocation-heavy helper objects.

Principal files:

- `src/java/patternfinder/PatternFinder.java`
- `src/java/patternfinder/PatUtils.java`
- `src/backend/cpp/wrapper.cpp`
- `src/backend/cpp/vary_cs.cpp`
- `src/backend/cpp/vary3.cpp`
- `src/backend/cpp/vary4.cpp`
- `src/java/billiards/geometry/Projectable.java`
- `src/java/billiards/geometry/ConvexPolygon.java`
- `src/java/billiards/geometry/LineSegment.java`
- `src/java/billiards/geometry/Rectangle.java`

# 15. Test And Diagnostic Code Added With The Changes

The significant behavioral changes are accompanied by targeted code that makes
their contracts executable.

## Native Cases

- Code-sequence tests verify that every rotation and reversal reaches the same
  canonical representative.
- MRR worker-count tests compare interval endpoints, boundary equations, and
  `LeftRight` witnesses.
- The long MRR workload can be enabled separately because it is too expensive
  for every short test run.
- Native fixtures fail when their input files are missing or empty instead of
  passing after running zero examples.

## Java Cases

- AutoPolyVary index tests cover forward ranges, reverse ranges, terminal
  boundaries, and zero-step rejection.
- The render-order test captures the continuation and proves coordinate work
  cannot run until render completion invokes it.
- Window tests verify both a missing finish observer and an initialized finish
  observer.

## Correctness-First Timing Workloads

The timing harness treats output identity as a prerequisite. The long MRR digest
includes interval bounds, equation text, and `LeftRight` provenance rather than
only elapsed time or displayed points. Samples with different digests are not
valid speed comparisons.

Timing runs are isolated by process and record worker count, wall time, CPU time
when available, and peak working set. This helps distinguish a real algorithmic
improvement from warm caches, nested oversubscription, or an invalid shortcut.

Principal files:

- `src/test/headers/equations_test.hpp`
- `src/test/headers/code_sequence_test.hpp`
- `src/test/java/billiards/viewer/ViewerAutoPolyVaryTest.java`
- `src/test/java/billiards/viewer/IterateToLimitWindowTest.java`
- `src/test/resources/empty_codes_to_15.txt`
- `src/test/resources/nonempty_codes_to_15.txt`
- `tools/benchmark/`

# 16. Significant File Map

| Area | Principal source files |
| --- | --- |
| MRR construction | `equations.cpp`, `unfolding.cpp`, `bounding_inequalities.cpp` |
| Polygon refinement | `refine.cpp`, `refine.hpp` |
| Native worker control | `utils.hpp`, `wrapper.cpp`, `wrapper.hpp` |
| Native Vary | `vary_cs.cpp`, `vary3.cpp`, `vary4.cpp`, `triangle_billiard4.cpp` |
| Numeric evaluation | `newton.hpp`, `intersection.hpp`, `evaluator.cpp/.hpp`, `common.cpp`, `verify.cpp` |
| Code canonicalization | `code_sequence.cpp`, `code_sequence.hpp` |
| Native/Java boundary | `wrapper.cpp/.hpp`, `Wrapper.java`, `CInfoAll.java` |
| Java storage/database | `Database.java`, `ConnectionPool.java` |
| Main rendering and workflows | `Viewer.java`, `Main.java`, `Utils.java` |
| AutoVary and Vary tasks | `PolyVaryTask.java`, `CycleVaryTask.java`, `GracefullyCancelable.java` |
| Cancellation and progress | `Progress.java`, `ProgressWithStatus.java`, `ProgressMultiTask.java`, draw-task classes |
| Window lifecycle | `IterateToLimitWindow.java`, `CoverWindow.java`, `SmallCoverWindow.java`, `StablesWindow.java` |
| Cover | `cover/save.cpp/.hpp`, `CoverStuff.java`, `CoverWindow.java` |
| Pattern output | `PatternFinder.java`, `PatUtils.java` |
| Geometry | `Projectable.java`, `ConvexPolygon.java`, `LineSegment.java`, `Rectangle.java` |
| Regression and timing code | `src/test/`, `tools/benchmark/` |

# 17. Remaining Technical Limits

The changes above correct specific defects and remove specific bottlenecks. They
do not imply that every concurrent or mathematical path is complete.

Important remaining limits include:

- the Java storage cache is bounded but is not yet a per-code single-flight
  registry;
- native Vary still depends on one process-wide cancellation flag, which is why
  outer Vary calls remain serialized;
- boundary-producing MRR refinement remains ordered because the result type does
  not support a provenance-preserving parallel merge;
- some UI workflows still need long interactive runs to exercise cancellation,
  reflection, window closure, and thousands of AutoPolyVary coordinates;
- cover artifact parsing now protects indexes but does not yet validate every
  stable/unstable role before later casts;
- several older native entry points catch `std::runtime_error` rather than all
  `std::exception` values; allocation and other non-runtime failures still need
  a complete no-exception-across-JNA audit;
- evaluator reuse reduces allocation churn, but native memory behavior should
  still be measured over repeated large cover and OSNO sessions;
- one or two timing samples are diagnostic smoke runs, not a general performance
  claim.

The most important design rule established by these changes is that concurrency
must follow the full state model. Independent curve generation, vertex
classification, row rendering, and candidate verification can run in parallel.
State transitions that publish boundary provenance, coordinate ownership, image
generation, terminal status, or global cancellation must remain explicitly
ordered or exclusively owned.
