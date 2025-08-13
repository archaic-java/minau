package work.archaic.minau;

import java.util.ArrayList;
import java.util.List;

final class Result {
  int suites = 0;
  int tests = 0;
  int passed = 0;
  int failures = 0;
  final List<String> failureMessages = new ArrayList<>();
  final List<Long> testDurations = new ArrayList<>();

  void recordSuite() {
    suites++;
  }

  void recordTest() {
    tests++;
  }

  void recordPass() {
    passed++;
  }
  
  void recordTestDuration(long durationMs) {
    testDurations.add(durationMs);
  }

  void recordFailure(String suiteName, String testName, Throwable error) {
    failures++;
    String message = String.format("%s#%s: %s", suiteName, testName, error.toString());
    failureMessages.add(message);
  }

  void recordSetupFailure(String suiteName, Throwable error) {
    String message = String.format("%s setup(): %s", suiteName, error.toString());
    failureMessages.add(message);
  }

  void recordTeardownFailure(String suiteName, Throwable error) {
    String message = String.format("%s teardown(): %s", suiteName, error.toString());
    failureMessages.add(message);
  }
  
  long getMinDuration() {
    return testDurations.stream().min(Long::compare).orElse(0L);
  }
  
  long getMaxDuration() {
    return testDurations.stream().max(Long::compare).orElse(0L);
  }
  
  double getAverageDuration() {
    if (testDurations.isEmpty()) return 0.0;
    return testDurations.stream().mapToLong(Long::longValue).average().orElse(0.0);
  }
}
