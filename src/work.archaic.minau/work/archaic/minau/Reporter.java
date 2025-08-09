package work.archaic.minau;

import java.time.Duration;

final class Reporter {

  static void printSummary(Result result, Duration duration) {
    System.out.println();
    System.out.println("--- SUMMARY " + "-".repeat(50));
    System.out.printf(
        "Suites: %d, Tests: %d, Passed: %d, Failed: %d, Time: %d ms%n",
        result.suites, result.tests, result.passed, result.failures, duration.toMillis());

    if (!result.failureMessages.isEmpty()) {
      System.out.println("Failures:");
      for (String message : result.failureMessages) {
        System.out.println("  - " + message);
      }
    }

    System.out.println("-".repeat(62));
  }

  static void printTestResult(
      String suiteName, String testName, boolean passed, long durationMs, Throwable error) {
    String symbol = passed ? "✓" : "✗";
    String timing = String.format(" (%d ms)", durationMs);

    if (passed) {
      System.out.println(symbol + " " + suiteName + "#" + testName + timing);
    } else {
      System.out.println(symbol + " " + suiteName + "#" + testName + " -> " + error.toString());
    }
  }
}
