package work.archaic.minau;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;

final class Result {
  int suites;
  int passed;
  int failures;
  final List<String> failureMessages = new ArrayList<>();
  final LongSummaryStatistics durations = new LongSummaryStatistics();

  void recordSuite() { suites++; }

  void record(TestOutcome outcome) {
    durations.accept(outcome.durationMs());
    if (outcome.passed()) passed++;
    else recordFailure(Reporter.caseFailure(outcome.suite(), outcome.name(),
        outcome.error(), outcome.evidence()));
  }

  void recordFailure(String message) {
    failures++;
    failureMessages.add(message);
  }

  long tests() { return durations.getCount(); }
  long minDuration() { return tests() == 0 ? 0 : durations.getMin(); }
  long maxDuration() { return tests() == 0 ? 0 : durations.getMax(); }
}
