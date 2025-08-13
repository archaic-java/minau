package work.archaic.minau;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Reporter {

  private static final Logger logger = LoggerFactory.getLogger(Reporter.class);

  static void printSummary(Result result, Duration duration) {
    double passRate = result.tests > 0 ? (result.passed * 100.0 / result.tests) : 0;
    double avgDuration = result.tests > 0 ? duration.toMillis() / (double) result.tests : 0;
    long minDuration = result.getMinDuration();
    long maxDuration = result.getMaxDuration();
    
    String summary = """
        
        Test Results:
        ├─ Suites:     %d
        ├─ Tests:      %d
        ├─ Passed:     %d (%.0f%%)
        ├─ Failed:     %d
        ├─ Duration:   %d ms (avg: %.1f ms/test)
        └─ Test Times: min: %d ms, max: %d ms""".formatted(
            result.suites,
            result.tests,
            result.passed, passRate,
            result.failures,
            duration.toMillis(), avgDuration,
            minDuration, maxDuration
        );
    
    if (!result.failureMessages.isEmpty()) {
      StringBuilder failureSection = new StringBuilder("\n\nFailures:");
      for (String message : result.failureMessages) {
        failureSection.append("\n   • ").append(message);
      }
      summary += failureSection.toString();
    }
    
    logger.info(summary);
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
