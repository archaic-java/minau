# Run, list and select tests

Read this when invoking Minau, rerunning a failure, or changing CLI options.
For module resolution and visibility, read [discovery](discovery.md).

## Invocation

```sh
java -ea --module-path out --add-modules example.test \
    -m work.archaic.minau/work.archaic.minau.Main example.test [options]
```

JVM flags precede `-m`; Minau's module list and options follow the main class.
The module list identifies scan targets; `--add-modules` resolves them for the JVM.

Pass comma-separated module names to scan multiple modules. Add `--debug` for completion
status. Exit code is 0 on success, 1 on discovery/test/suite failure and 2 for invalid CLI arguments
or selections. Discovery failures include guidance for unresolved `--add-modules` targets.
Assertions are checked at startup; launch without `-ea` fails.

## List and select

Use `--list` to register and describe cases without running case bodies. The listing includes
the suite class, one-based registration ordinal and copyable `--suite`/`--case` flags:

```sh
java -ea --module-path out --add-modules com.example.foo.test \
    -m work.archaic.minau/work.archaic.minau.Main com.example.foo.test --list
java -ea --module-path out --add-modules com.example.foo.test \
    -m work.archaic.minau/work.archaic.minau.Main com.example.foo.test \
    --suite com.example.foo.test.AdditionCases --case 2
```

`--suite` selects a fully qualified v01 or v02 suite before excluded suites are constructed.
`--case` requires a v02 `--suite`; it selects one registered case while validating the entire
selected registration. v01 methods can be listed or selected as a suite, but have no v02 case
ordinal. An ordinal is reproducible only while registration order and inputs remain unchanged;
it is not a permanent test ID. Missing or invalid selection and unknown flags exit with status 2.

Listing constructs and registers selected v02 suites, so registration can fail.
It is not a side-effect-free inspection of source code. Acquire resources in case
bodies so listing does not acquire them.

## Change selection or flags

Change [Options.java](../../../src/work.archaic.minau/work/archaic/minau/Options.java)
for parsing and suite selection, and
[Main.java](../../../src/work.archaic.minau/work/archaic/minau/Main.java)
for orchestration, usage text and exit codes. Case ordinal validation and listing
belong to [CaseExecutor.java](../../../src/work.archaic.minau/work/archaic/minau/CaseExecutor.java).

Preserve this ordering: discover all requested modules, select suites, register the
selected v02 suite completely, then select an ordinal. Excluded suites must not be
constructed. Do not deduplicate registrations by description or instance identity.

Extend [IntegrationTest.java](../../../src/work.archaic.minau.test/work/archaic/minau/test/IntegrationTest.java)
with real CLI invocations. Cover successful execution, listing without case-body
markers, duplicate cases, invalid values/combinations and exit codes. Use
[DiscoveryTest.java](../../../src/work.archaic.minau.test/work/archaic/minau/test/DiscoveryTest.java)
when a selection change could conceal discovery errors in another requested module.
Update this reference and `Main.usage()` together when adding a flag.
