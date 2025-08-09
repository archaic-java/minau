package work.archaic.minau;

import java.util.ArrayList;
import java.util.List;

final class Result {
  int suites = 0;
  int tests = 0;
  int passed = 0;
  int failures = 0;
  final List<String> failureMessages = new ArrayList<>();

  void recordSuite() {
    suites++;
  }

  void recordTest() {
    tests++;
  }

  void recordPass() {
    passed++;
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
}
