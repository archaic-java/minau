package work.archaic.minau;

import java.time.Duration;

final class Reporter {

  static void printSummary(Result result, Duration duration) {
    double passRate = result.tests > 0 ? (result.passed * 100.0 / result.tests) : 0;
    double avgDuration = result.tests > 0 ? duration.toMillis() / (double) result.tests : 0;
    long minDuration = result.getMinDuration();
    long maxDuration = result.getMaxDuration();

    String summary =
        """

        Test Results:
        ├─ Suites:     %d
        ├─ Tests:      %d
        ├─ Passed:     %d (%.0f%%)
        ├─ Failed:     %d
        ├─ Duration:   %d ms (avg: %.1f ms/test)
        └─ Test Times: min: %d ms, max: %d ms\
        """
            .formatted(
                result.suites,
                result.tests,
                result.passed,
                passRate,
                result.failures,
                duration.toMillis(),
                avgDuration,
                minDuration,
                maxDuration);

    if (!result.failureMessages.isEmpty()) {
      StringBuilder failureSection = new StringBuilder("\n\nFailures:");
      for (String message : result.failureMessages) {
        failureSection.append("\n   • ").append(message);
      }
      summary += failureSection.toString();
    }

    System.out.println(summary);
  }

  static void printTestResult(
      String suiteName, String testName, boolean passed, long durationMs, Throwable error, boolean debug) {
    if (debug) {
      if (passed) {
        System.out.println("Running '" + suiteName + "'-suite, test: '" + testName + "'");
      } else {
        System.out.println("✗ " + suiteName + "#" + testName + " -> " + error.toString());
      }
    }
  }
}
