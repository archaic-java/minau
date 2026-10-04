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
        assert actual == expected : "Sum must equal " + expected + "; got " + actual;
    }
}
```

Export the suite package to Minau in the test module:

```java
module example.test {
    requires work.archaic.service.catalog;
    exports example.test to work.archaic.minau;
}
```

After compiling your test module and its dependencies, run it with:

```sh
java -ea --module-path out --add-modules example.test \
    -m work.archaic.minau/work.archaic.minau.Main example.test
```

For registration, resource ownership and existing v01 suites, read
[writing tests](skills/maintain-minau/references/writing-tests.md).
For listing or rerunning one case, read the
[CLI reference](skills/maintain-minau/references/cli.md).

## Build and verify this repository

Check out `archaic-java/service-catalog` as sibling `service-catalog`, with the v02 test
contracts available on its main branch. The checked-in module source link points there.
No logging provider is required. With JDK 25 on PATH:

```sh
javac @cmd/compile
java @cmd/test
java @cmd/run
```

Run these commands from the repository root. A full JDK, including `javac` and `jar`,
is required. The [contributor guide](skills/maintain-minau/references/contributing.md)
explains the source map, regression fixtures and troubleshooting.

## Understand or change Minau

Start with the [Minau project skill](skills/maintain-minau/SKILL.md). It is a shared
reading map for human contributors and coding agents: read its short foundation,
then open only the references relevant to your task. No skill installation is needed
to follow these Markdown links. Agents whose tool supports local skills can use the
folder in place; `AGENTS.md` points to it even without automatic skill discovery.

The skill complements Archaic Java conventions with Minau-specific responsibilities
and verification. Detailed behavior lives in the linked references; service API
contracts remain in service-catalog Javadoc. Update the owning document with code
changes rather than copying it into another guide.
