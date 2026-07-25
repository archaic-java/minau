package work.archaic.minau;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import work.archaic.service.test.v01.TestSuite;

final class TestExecutor {

  record ExecutionResult(List<TestResult> tests, List<String> failures) {
    int suiteCount() {
      return (int) tests.stream().map(TestResult::suiteName).distinct().count();
    }

    int testCount() {
      return tests.size();
    }

    int passedCount() {
      return (int) tests.stream().filter(TestResult::passed).count();
    }

    int failedCount() {
      return testCount() - passedCount();
    }

    long durationMs() {
      return tests.stream().mapToLong(TestResult::durationMs).sum();
    }
  }

  record TestResult(
      String suiteName, String testName, boolean passed, long durationMs, Throwable error) {}

  // ---------------------------------------------------------------------------
  // Backwards compatibility for the old mutable Result API.
  // Remove this block once all callers use executeTests(List<TestDescriptor>).
  // ---------------------------------------------------------------------------

  static void executeTests(List<TestDescriptor> tests, Result result, boolean debug) {
    var execution = executeTests(tests, debug);

    for (var ignored : execution.tests().stream().map(TestResult::suiteName).distinct().toList()) {
      result.recordSuite();
    }

    for (var test : execution.tests()) {
      result.recordTest();
      result.recordTestDuration(test.durationMs());
      if (test.passed()) {
        result.recordPass();
      } else {
        result.recordFailure(test.suiteName(), test.testName(), test.error());
      }
    }

    for (var failure : execution.failures()) {
      result.failureMessages.add(failure);
      result.failures++;
    }
  }

  // ---------------------------------------------------------------------------

  static ExecutionResult executeTests(List<TestDescriptor> tests, boolean debug) {
    var testsByClass = new HashMap<Class<?>, List<TestDescriptor>>();
    for (var test : tests) {
      testsByClass.computeIfAbsent(test.testClass, ignored -> new ArrayList<>()).add(test);
    }

    var results = new ConcurrentLinkedQueue<TestResult>();
    var failures = new ConcurrentLinkedQueue<String>();
    var threads = new ArrayList<Thread>();

    for (var entry : testsByClass.entrySet()) {
      var testClass = entry.getKey();
      var suiteTests = entry.getValue();

      threads.add(
          Thread.ofVirtual()
              .name("suite-" + testClass.getSimpleName())
              .start(() -> runSuite(testClass, suiteTests, results, failures, debug)));
    }

    joinAll(threads, "Suite execution was interrupted");
    return new ExecutionResult(new ArrayList<>(results), new ArrayList<>(failures));
  }

  private static void runSuite(
      Class<?> testClass,
      List<TestDescriptor> tests,
      ConcurrentLinkedQueue<TestResult> results,
      ConcurrentLinkedQueue<String> failures,
      boolean debug) {
    var suiteName = testClass.getSimpleName();
    Object instance;

    try {
      instance = testClass.getDeclaredConstructor().newInstance();
      ((TestSuite) instance).setup();
    } catch (Throwable error) {
      failures.add("%s setup(): %s".formatted(suiteName, error));
      recordSetupFailure(suiteName, tests, error, results, debug);
      return;
    }

    tests.sort((left, right) -> left.methodName.compareTo(right.methodName));
    runTests(instance, tests, suiteName, results, failures, debug);

    try {
      ((TestSuite) instance).teardown();
    } catch (Throwable error) {
      failures.add("%s teardown(): %s".formatted(suiteName, error));
    }
  }

  private static void recordSetupFailure(
      String suiteName,
      List<TestDescriptor> tests,
      Throwable error,
      ConcurrentLinkedQueue<TestResult> results,
      boolean debug) {
    for (var test : tests) {
      var result = new TestResult(suiteName, test.methodName, false, 0, error);
      results.add(result);
      print(result, debug);
    }
  }

  private static void runTests(
      Object instance,
      List<TestDescriptor> tests,
      String suiteName,
      ConcurrentLinkedQueue<TestResult> results,
      ConcurrentLinkedQueue<String> failures,
      boolean debug) {
    var threads = new ArrayList<Thread>();

    for (var test : tests) {
      Runnable runAndRecordTest =
          () -> {
            var result = runTest(instance, test, suiteName);
            results.add(result);
            print(result, debug);
          };

      threads.add(
          Thread.ofVirtual()
              .name("test-" + suiteName + "#" + test.methodName)
              .start(runAndRecordTest));
    }

    joinAll(threads, "Test execution was interrupted");
  }

  private static TestResult runTest(Object instance, TestDescriptor test, String suiteName) {
    var start = System.nanoTime();
    Throwable error = null;

    try {
      MethodHandle method = test.methodHandle.bindTo(instance);
      method.invokeExact();
    } catch (Throwable thrown) {
      error = thrown instanceof InvocationTargetException wrapped ? wrapped.getCause() : thrown;
    }

    var durationMs = (System.nanoTime() - start) / 1_000_000;
    return new TestResult(suiteName, test.methodName, error == null, durationMs, error);
  }

  private static void print(TestResult result, boolean debug) {
    Reporter.printTestResult(
        result.suiteName(), result.testName(), result.passed(), result.durationMs(), result.error(), debug);
  }

  private static void joinAll(List<Thread> threads, String message) {
    for (var thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(message, error);
      }
    }
  }
}
