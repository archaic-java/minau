# Minau

A Java test runner where each test executes on its own virtual thread. Tests depend on
`work.archaic.service.catalog`; Minau discovers suites in the selected modules and reports
results through its CLI. Use JDK 25 and enable assertions.

## Record-based tests (v02)

Organize a file around a public suite record and package-private case records:

```java
package example.test;

import java.util.Collection;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestSuite;
import work.archaic.service.test.v02.TestTrail;

public record ArithmeticTests() implements TestSuite {
    @Override
    public void cases(Collection<TestCase> cases) {
        cases.add(new Addition(2, 3, 5));
        cases.add(new Addition(-1, 1, 0));
        for (int value = 0; value < 5; value++) {
            cases.add(new Addition(value, 0, value));
        }
    }
}

record Addition(int left, int right, int expected) implements TestCase {
    @Override
    public void run(TestTrail trail) {
        int actual = Math.addExact(left, right);
        trail.note("Actual sum: " + actual);
        assert actual == expected : "Expected sum: " + expected;
    }
}
```

Each registration is one test. Minau uses the suite's full class name, a registration ordinal
and the case's `toString()` (the default record representation works well) for reporting.
Duplicate instances and descriptions remain separate tests. Records are recommended, not required.
Top-level case record names must be unique within the package.

The suite receives a fresh mutable collection. Register synchronously, then relinquish the
collection. Minau snapshots and validates it before starting any of that suite's cases.
Null entries, failed constructors, failed registration and failed description generation are
reported as suite failures without running a partial suite. Empty suites count as suites with
zero tests. Suite failures affect the exit code and failure count without inventing case results.

Cases execute concurrently on virtual threads. Keep case data immutable; create fixtures and
acquire/close resources in `run`. Records do not make referenced objects immutable. v02 has no
suite-level setup/teardown hooks. Expected exceptions should be caught and checked in the case.
Normal return passes; any escaping exception or error fails and retains the original stack
trace, causes and suppressed exceptions.

### Trails

`trail.note(...)` records evidence that appears only if the case fails. Successful evidence is
discarded. Each execution has its own trail, valid only on the executing thread while `run`
is active; other-thread or completed-trail calls fail explicitly. Null notes are rejected.
There is no propagation to child threads. Complete asynchronous work before returning.

Minau keeps the latest 256 notes, each limited to 2048 UTF-16 code units without splitting a
surrogate pair. Reports state the number of evicted notes and all truncated note submissions
(including those later evicted). Newlines in notes and case names are escaped. Retention bounds
cover the stored notes, not input strings or throwable graphs. Capture occurs even on success.
Failure reports are rendered together in the final summary, in registration order within each
v02 suite. `--debug` also prints completion status as each case finishes.

Test trails are independent of application logging and do not depend on Peep. Application goals
can execute within a case without colliding with Minau's diagnostic context.

## Modules and discovery

Minau continues scanning `out/<module-name>` for concrete TestSuite implementations. Run from
the project root with the selected modules resolved via `--add-modules`. Each suite must have
a public no-argument constructor. A public zero-component record provides one automatically.
For v02, export the suite package (a qualified export is sufficient):

```java
module example.test {
    requires work.archaic.service.catalog;
    exports example.test to work.archaic.minau;
}
```

Package-private case implementations are invoked through TestCase and need no `opens`.
Existing v01 suites with `@Test`, setup and teardown continue to work alongside v02 suites.
Keep their qualified `opens ... to work.archaic.minau` for annotated-method discovery.
Do not implement both TestSuite versions on one class; v02 takes precedence in discovery.

```sh
java -ea --module-path out --add-modules example.test \
    -m work.archaic.minau/work.archaic.minau.Main example.test
```

Pass comma-separated module names to scan multiple modules. Add `--debug` for completion
status. Exit code is 0 on success, 1 on test/suite failure and 2 for missing CLI arguments.
Assertions are checked at startup; launch without `-ea` fails.

## Build and verify

Check out `archaic-java/service-catalog` as sibling `service-catalog`, with the v02 test
contracts (the matching `test-v02-record-cases` branch until merged). The checked-in module source link points there.
No logging provider is required. With JDK 25 on PATH:

```sh
javac @cmd/compile
java @cmd/test
java @cmd/run
```

`cmd/test` runs isolated CLI checks covering v01/v02 coexistence, exported packages without
opens, package-private records, duplicate registration, concurrent execution, snapshot
isolation, failure evidence, trail bounds and thread confinement, registration failures,
empty suites and assertion enforcement. `cmd/run` runs the examples, including data-driven
record cases alongside the existing annotated suites.
