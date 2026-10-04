# Maintain and verify Minau

Read this before implementing a runner change or extending its regression harness.
Use the canonical [build and verification commands](../../../README.md#build-and-verify-this-repository).

## Contents

- [Repository conventions](#repository-conventions)
- [Source map](#source-map)
- [Make a change](#make-a-change)
- [Extend regression coverage](#extend-regression-coverage)
- [Diagnose the environment](#diagnose-the-environment)
- [Keep documentation authoritative](#keep-documentation-authoritative)

## Repository conventions

Minau follows Archaic Java: JDK 25, explicit JPMS modules, JDK-first implementations,
and checked-in `javac`/`java` argument files. Use the Archaic Java skill when available
for broader design guidance; the rules here and in the repository are sufficient to
work without installing it. Do not introduce Maven, Gradle, class-path fallbacks,
automatic modules or reflective access workarounds.

Keep runner internals package-private and its JPMS surface narrow. Test code depends
on versioned service contracts. Published contract versions remain compatible; propose
a new version in service-catalog if the needed behavior changes that contract.
Keep assertions inline with explanations, as described in [writing tests](writing-tests.md).

## Source map

Links below point to source declarations rather than duplicating their implementation.

| Responsibility | Source |
|---|---|
| Startup, assertions, discovery/selection orchestration and process exit | [Main](../../../src/work.archaic.minau/work/archaic/minau/Main.java) |
| CLI parsing and suite filtering | [Options](../../../src/work.archaic.minau/work/archaic/minau/Options.java) |
| Resolved-module scanning and declaration validation | [ModuleScanner](../../../src/work.archaic.minau/work/archaic/minau/ModuleScanner.java) |
| v02 registration snapshot, naming, listing and concurrent cases | [CaseExecutor](../../../src/work.archaic.minau/work/archaic/minau/CaseExecutor.java) |
| v01 shared-suite lifecycle and concurrent methods | [TestExecutor](../../../src/work.archaic.minau/work/archaic/minau/TestExecutor.java) |
| Thread-owned failure evidence | [CaseTrail](../../../src/work.archaic.minau/work/archaic/minau/CaseTrail.java) |
| Completed case data, counts and presentation | [TestOutcome](../../../src/work.archaic.minau/work/archaic/minau/TestOutcome.java), [Result](../../../src/work.archaic.minau/work/archaic/minau/Result.java), [Reporter](../../../src/work.archaic.minau/work/archaic/minau/Reporter.java) |

The runner depends on `work.archaic.service.catalog`. Its
[module descriptor](../../../src/work.archaic.minau/module-info.java) declares that
boundary. `com.example.foo` and `com.example.foo.test` demonstrate using the contracts;
`work.archaic.minau.fixture` supplies cases for runner regression tests;
`work.archaic.minau.test` drives the real CLI in child JVMs. The test module launches
as an ordinary main program so it can independently check runner startup and exit.

The service-catalog source link requires the sibling checkout documented in the README.
The repository also retains a `lib/src/work.archaic.jules` link, but the current compile
module list and runner descriptor do not require it. Do not add a logging provider
merely to run Minau.

## Make a change

1. Check the worktree, `java --version`, `javac --version`, module descriptors and
   `cmd/`. Read the relevant behavioral reference from [the skill](../SKILL.md).
2. Decide whether the behavior belongs in Minau's CLI/implementation or the shared
   service contract. Read the linked contract Javadoc before changing API assumptions.
3. Change the smallest owning component. Keep discovery, registration and execution
   distinct; choose regression fixtures that expose the changed boundary.
4. Compile, run the repository integration checks, and run the examples with the
   README commands. Update descriptors and command files if adding a module.
5. Update the owning reference, inspect the diff, and report the checks actually run.
   Keep generated classes, temporary fixtures and downloaded tools out of commits.

## Extend regression coverage

`cmd/test` runs isolated CLI checks covering v01/v02 coexistence, exported packages without
opens, package-private records, duplicate registration, concurrent execution, snapshot
isolation, failure evidence, trail bounds and thread confinement, registration failures,
empty suites and assertion enforcement. It also compiles isolated modular fixtures and verifies
exploded/JAR discovery from another directory, unresolved and empty modules, missing runtime
dependencies, invalid declarations, JPMS access diagnostics, discovery-before-execution and
v01 setup/concurrent-method/teardown behavior. The tests require a full JDK (including `javac`
and `jar`). `cmd/run` runs the examples, including data-driven
record cases alongside the existing annotated suites.

Use [IntegrationTest.java](../../../src/work.archaic.minau.test/work/archaic/minau/test/IntegrationTest.java)
with [RegisteredCases.java](../../../src/work.archaic.minau.fixture/work/archaic/minau/fixture/RegisteredCases.java)
for execution, registration, evidence and CLI-selection regressions. Fixture modes
are selected through the child JVM's `fixture` system property.
Use [DiscoveryTest.java](../../../src/work.archaic.minau.test/work/archaic/minau/test/DiscoveryTest.java)
for declarations and JPMS boundaries: its `Source` entries compile into temporary
modules, and its workspace packages JARs and launches from outside the repository.
`IntegrationTest.main` invokes `DiscoveryTest.run`; `cmd/test` covers both.

Prefer externally observable checks: exit status, contextual diagnostics, and markers
showing whether registration or a case body ran. Use latches for concurrency checks
and time limits for child processes. Keep temporary files and child processes owned
by the harness and clean them on failure. Do not expose runner internals just to test
them or add assertions that merely restate the implementation.

## Diagnose the environment

| Symptom | Check |
|---|---|
| Compiler or runtime cannot use JDK 25 APIs | Both `java` and `javac` must come from a full JDK 25 installation. |
| Catalog module or v02 types are missing | Resolve `lib/src/work.archaic.service.catalog` and inspect the sibling checkout. |
| Changed or deleted suites still appear | Remove generated `out/` and compile again; discovery reads compiled resources. |
| A suite cannot be found or accessed | Read [discovery](discovery.md); check resolution, public constructors and exports/opens. |
| Assertion enforcement aborts startup | Use `-ea`; `cmd/test` and `cmd/run` already include it. |
| Evidence is missing or mixed | Read [diagnostics](diagnostics.md); check trail lifetime and executing thread. |

For documentation-only changes, check local links, skill metadata and consistency
against command files, source and contract Javadoc. Run Java checks if examples or
claims introduce behavior that needs execution to verify; report any unavailable JDK.

## Keep documentation authoritative

Keep purpose, the first example and canonical repository commands in `README.md`.
Keep essential constraints and task routing in `SKILL.md`, and behavior and rationale
in the owning reference. Keep service API contracts in service-catalog Javadoc and
implementation details beside their declarations. Link between those locations rather
than copying full contracts or maintaining parallel human/agent guides.

When changing discovery, execution or reporting, update the corresponding explanation
and its regression guidance in the same change. Record the reason for a consequential
boundary alongside the behavior; do not invent historical decisions from assumptions.
