# Failure evidence and reporting

Read this when investigating missing evidence, changing trails, or extending output.
For case resource ownership, read [writing tests](writing-tests.md).

## Trail lifetime and retention

`trail.note(...)` records evidence that appears only if the case fails. Successful evidence is
discarded. Each execution has its own trail, valid only on the executing thread while `run`
is active; other-thread or completed-trail calls fail explicitly. Null notes are rejected.
There is no propagation to child threads. Complete asynchronous work before returning.

Minau keeps the latest 256 notes, each limited to 2048 UTF-16 code units without splitting a
surrogate pair. Reports state the number of evicted notes and all truncated note submissions
(including those later evicted). Newlines in notes and case names are escaped. Retention bounds
cover the stored notes, not input strings or throwable graphs. Capture occurs even on success.

## Reports and timing

Failure reports are rendered together in the final summary, in registration order within each
v02 suite. Both execution models retain full stack traces, causes and suppressed exceptions;
v01 suite names are fully qualified and its methods are reported in name order. The summary's
average test duration is the mean of measured case durations, independent of parallel execution;
the total duration is the wall-clock run time. `--debug` also prints completion status as each
case finishes.

## Keep application logging independent

Test trails are independent of application logging and do not depend on Peep. Application goals
can execute within a case without colliding with Minau's diagnostic context.

## Change and verify reporting

Inspect [CaseTrail.java](../../../src/work.archaic.minau/work/archaic/minau/CaseTrail.java)
for ownership, lifetime, bounds and discarded success evidence;
[CaseExecutor.java](../../../src/work.archaic.minau/work/archaic/minau/CaseExecutor.java)
for capturing the original failure and closing the trail;
[Reporter.java](../../../src/work.archaic.minau/work/archaic/minau/Reporter.java)
for escaping and rendering; and
[Result.java](../../../src/work.archaic.minau/work/archaic/minau/Result.java)
for counts and duration aggregation.

Registration failures contribute failures without inventing case durations or test
results. Preserve that distinction when changing summary statistics. Execution order
is concurrent; final evidence order must remain reproducible within a suite.

Extend the diagnostic fixtures in
[RegisteredCases.java](../../../src/work.archaic.minau.fixture/work/archaic/minau/fixture/RegisteredCases.java)
and check their CLI output in
[IntegrationTest.java](../../../src/work.archaic.minau.test/work/archaic/minau/test/IntegrationTest.java).
Verify successful evidence disappears, failed cases retain their own ordered notes,
trail loss is visible, surrogate pairs remain intact, and original stack traces,
causes and suppressed exceptions survive rendering. Exercise both test versions when
changing shared reporting.
