package work.archaic.minau;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Reporter {

  private static final Logger logger = LoggerFactory.getLogger(Reporter.class);

  static void printSummary(Result result, Duration duration) {
    StringBuilder summary = new StringBuilder();
    summary.append("\n");
    summary.append("--- SUMMARY ").append("-".repeat(50)).append("\n");
    summary.append(String.format("Suites: %d, Tests: %d, Passed: %d, Failed: %d, Time: %d ms%n",
        result.suites, result.tests, result.passed, result.failures, duration.toMillis()));

    if (!result.failureMessages.isEmpty()) {
      summary.append("Failures:\n");
      for (String message : result.failureMessages) {
        summary.append("  - ").append(message).append("\n");
      }
    }

    summary.append("-".repeat(62));
    logger.info(summary.toString());
  }

  static void printTestResult(
      String suiteName, String testName, boolean passed, long durationMs, Throwable error) {
    if (passed) {
      logger.debug("Running '{}'-suite, test: '{}'", suiteName, testName);
    } else {
      logger.info("✗ {}#{} -> {}", suiteName, testName, error.toString());
    }
  }
}
