# Correctness-first benchmark harness

The runner measures named native workloads and stores raw evidence under
`build/benchmarks/<timestamp>/`. The large workload is the exact CS involved in
the 2026-07-15 MRR repair. It calculates once per requested worker count and
prints a hash of interval endpoints, equations, and `LeftRight` provenance.

Run a quick workload:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  .\tools\benchmark\run-benchmarks.ps1 `
  -Workload mrr-small-worker-contract
```

Run the large worker matrix with fewer initial samples:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  .\tools\benchmark\run-benchmarks.ps1 `
  -Workload reported-long-cs-mrr -Samples 2 -WorkerCounts 1,2,4
```

Use at least five measured samples for a reported conclusion. The runner builds
once before timing, supports warmups, captures raw stdout/stderr, records Git,
JVM, OS, CPU, and build-profile metadata, and fails if correctness output is
missing or result hashes disagree. `-BuildProfile debug` exists for profiling
and debugger work; do not compare its timings with optimized profiles.
When `JAVA_HOME` is set, the runner records the version from that exact Java
executable because Gradle uses it even if another `java.exe` appears first on
`Path`.

`-SkipBuild` is for runner smoke tests only. Because an existing executable may
have been produced with different flags, those sessions are labeled
`<profile>-unverified-skip-build` and cannot support a performance conclusion.

The corpus JSON files document provenance and expected invariants. Add actual
LiLuMaxVary, triples, cover, and LiPattern captures only after sanitizing paths,
database names, and unpublished research data.
