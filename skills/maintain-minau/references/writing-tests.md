# Write tests with Minau

Read this when adding a suite, choosing fixtures, or handling concurrency and failure.
Start from the [working v02 example](../../../README.md#record-based-tests-v02).
Use [module and discovery rules](discovery.md) to make the suite discoverable and the
[CLI reference](cli.md) to run or select it.

## Register independent v02 cases

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

## Assert behavior and own resources

Keep checks inline with `assert condition : "explanation";`. Explain the violated
expectation, and include observed values when useful. Do not introduce assertion
helper methods or wrappers in new tests. Use ordinary helpers to prepare inputs,
launch processes or collect observations when that makes the test clearer.

Catch an expected exception in the case and assert its relevant properties. Fail
explicitly if the operation returns normally. Do not catch an unexpected exception
merely to replace its original diagnostic evidence.

For integration tests, acquire external resources inside `run`, close them before
returning, and give concurrent cases separate mutable fixtures. Use synchronization
to observe ordering instead of assuming a scheduling delay. Put deadlines on child
processes and asynchronous work, and clean them up on failure as well as success.
See [testing Minau itself](contributing.md#extend-regression-coverage) for the current
CLI and modular-fixture harnesses.

## Preserve existing v01 suites

Existing annotated suites remain supported. They implement
`work.archaic.service.test.v01.TestSuite` and use `@Test` methods. Their methods share
one suite instance: setup precedes concurrent method execution, and teardown follows
completion. Make shared state safe for concurrent access. If construction or setup
fails, methods do not run and teardown is not called; teardown failures still fail
the run after successful methods. See the [discovery reference](discovery.md) for
method signatures and package access. Do not implement both suite versions on one type.

## Follow the service contract

The versioned interfaces and their Javadoc are maintained in
[service-catalog](https://github.com/archaic-java/service-catalog/tree/main/src/work.archaic.service.catalog/work/archaic/service/test).
Tests depend on that contract, not runner implementation classes. Read the checkout
linked by `lib/src/work.archaic.service.catalog` when deciding whether a change belongs
in the contract or only in Minau.
