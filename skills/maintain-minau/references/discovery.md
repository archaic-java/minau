# Modules and discovery

Read this when configuring test modules, changing discovery, or diagnosing a suite
that cannot be found. For launch flags and selection, read [CLI usage](cli.md).

## Resolve and expose suites

Minau discovers concrete TestSuite implementations in the explicitly selected, resolved JPMS
modules. Resolve them with `--add-modules`. Discovery uses `ModuleReader`, so both exploded modules
and modular JARs work, independently of the working directory or output directory name. Only
requested modules are scanned; their dependencies are not additional test targets. Each suite
must be public and have an accessible public no-argument constructor. A public zero-component
record provides one automatically.
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

## Validate before execution

Discovery validates every requested module before constructing suites or executing tests. Missing
resolved modules, unreadable resources, class loading or linkage failures, inaccessible suites or
methods, and invalid annotated method signatures fail discovery with module/class context and
original causes. A module without concrete suites also fails; an explicitly empty v01 or v02 suite
remains valid and appears in listings. Invalid `@Test` methods (static, non-void or with parameters)
are errors rather than silently ignored tests. For v01, keep the suite package open to Minau;
v02 suite packages need public access as shown above. Loading for discovery does not initialize
classes, but construction and registration may do so later.

Discovery errors across modules are reported together, and no case bodies run if any module
fails discovery. This applies to `--list` and precedes `--suite` selection. Runtime constructor
or registration exceptions remain suite failures; excluded suites are never constructed.
Discovery checks compiled contents, so use a clean compilation to avoid stale or omitted tests.

## Maintain the discovery boundary

Change [ModuleScanner.java](../../../src/work.archaic.minau/work/archaic/minau/ModuleScanner.java)
for module scanning and declaration/access validation. Keep loading separate from
construction: validating all requested modules first prevents a partly executed run
from concealing a broken module. Scan resolved module resources rather than assuming
an `out/` layout or walking the current directory; this preserves modular JAR support.

Exercise changes through [DiscoveryTest.java](../../../src/work.archaic.minau.test/work/archaic/minau/test/DiscoveryTest.java).
Use its temporary source modules to verify exported/open packages, missing runtime
types, invalid declarations and behavior from another working directory. For a new
validation rule, check both the contextual error and the absence of constructor,
registration and case-body markers. Check `--list` and selection as well as normal
execution: neither should bypass discovery validation.
