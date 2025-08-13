package work.archaic.minau;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import work.archaic.service.test.v01.TestSuite;

final class TestExecutor {

  private record TestResult(
      String suiteName, String testName, boolean passed, long durationMs, Throwable error) {}

  private record SuiteResult(
      String suiteName,
      int testCount,
      int passedCount,
      int failedCount,
      List<TestResult> testResults,
      List<String> failureMessages,
      boolean setupFailed,
      boolean teardownFailed,
      Throwable setupError,
      Throwable teardownError) {}

  static void executeTests(List<TestDescriptor> tests, Result result) {
    if (tests.isEmpty()) {
      return;
    }

    Map<Class<?>, List<TestDescriptor>> testsByClass = groupTestsByClass(tests);
    List<SuiteResult> suiteResults = executeClassesInParallel(testsByClass);
    aggregateResults(suiteResults, result);
  }

  private static Map<Class<?>, List<TestDescriptor>> groupTestsByClass(List<TestDescriptor> tests) {
    Map<Class<?>, List<TestDescriptor>> testsByClass = new HashMap<>();
    for (TestDescriptor test : tests) {
      testsByClass.computeIfAbsent(test.testClass, k -> new ArrayList<>()).add(test);
    }
    return testsByClass;
  }

  private static List<SuiteResult> executeClassesInParallel(
      Map<Class<?>, List<TestDescriptor>> testsByClass) {
    List<Thread> suiteThreads = new ArrayList<>();
    ConcurrentLinkedQueue<SuiteResult> suiteResults = new ConcurrentLinkedQueue<>();

    for (Map.Entry<Class<?>, List<TestDescriptor>> entry : testsByClass.entrySet()) {
      Thread virtualThread = createSuiteThread(entry, suiteResults);
      suiteThreads.add(virtualThread);
    }

    waitForAllThreads(suiteThreads);
    return new ArrayList<>(suiteResults);
  }

  private static Thread createSuiteThread(
      Map.Entry<Class<?>, List<TestDescriptor>> entry,
      ConcurrentLinkedQueue<SuiteResult> suiteResults) {
    Class<?> testClass = entry.getKey();
    List<TestDescriptor> classTests = entry.getValue();

    return Thread.ofVirtual()
        .name("suite-" + testClass.getSimpleName())
        .start(
            () -> {
              SuiteResult suiteResult = executeTestsInClass(testClass, classTests);
              suiteResults.offer(suiteResult);
            });
  }

  private static void waitForAllThreads(List<Thread> threads) {
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Suite execution was interrupted", e);
      }
    }
  }

  private static void aggregateResults(List<SuiteResult> suiteResults, Result result) {
    for (SuiteResult suiteResult : suiteResults) {
      result.recordSuite();
      aggregateTestResults(suiteResult.testResults, result);
      aggregateFailureMessages(suiteResult.failureMessages, result);
    }
  }

  private static void aggregateTestResults(List<TestResult> testResults, Result result) {
    for (TestResult testResult : testResults) {
      result.recordTest();
      result.recordTestDuration(testResult.durationMs);
      if (testResult.passed) {
        result.recordPass();
      }
    }
  }

  private static void aggregateFailureMessages(List<String> failureMessages, Result result) {
    for (String failureMessage : failureMessages) {
      result.failureMessages.add(failureMessage);
      result.failures++;
    }
  }

  private static SuiteResult executeTestsInClass(Class<?> testClass, List<TestDescriptor> tests) {
    String suiteName = testClass.getSimpleName();
    List<String> failureMessages = new ArrayList<>();

    // Try to create instance and run setup
    Object instance;
    try {
      instance = testClass.getDeclaredConstructor().newInstance();
      ((TestSuite) instance).setup();
    } catch (Throwable e) {
      String message = String.format("%s setup(): %s", suiteName, e.toString());
      failureMessages.add(message);
      List<TestResult> failedTests = createFailedTestResults(tests, suiteName, "Setup failed");
      return new SuiteResult(
          suiteName,
          tests.size(),
          0,
          tests.size(),
          failedTests,
          failureMessages,
          true,
          false,
          e,
          null);
    }

    // Sort tests by method name for deterministic execution
    tests.sort((a, b) -> a.methodName.compareTo(b.methodName));

    // Execute tests in parallel using virtual threads
    List<TestResult> testResults = executeTestsInParallel(instance, tests, suiteName);

    // Run teardown
    boolean teardownFailed = false;
    Throwable teardownError = null;
    try {
      ((TestSuite) instance).teardown();
    } catch (Throwable e) {
      teardownFailed = true;
      teardownError = e;
      String message = String.format("%s teardown(): %s", suiteName, e.toString());
      failureMessages.add(message);
    }

    // Calculate counts and collect failure messages
    int testCount = testResults.size();
    int passedCount = (int) testResults.stream().filter(tr -> tr.passed).count();
    int failedCount = testCount - passedCount;

    for (TestResult testResult : testResults) {
      if (!testResult.passed) {
        String message =
            String.format(
                "%s#%s: %s",
                testResult.suiteName, testResult.testName, testResult.error.toString());
        failureMessages.add(message);
      }
    }

    return new SuiteResult(
        suiteName,
        testCount,
        passedCount,
        failedCount,
        testResults,
        failureMessages,
        false,
        teardownFailed,
        null,
        teardownError);
  }

  private static List<TestResult> createFailedTestResults(
      List<TestDescriptor> tests, String suiteName, String reason) {
    List<TestResult> failedTests = new ArrayList<>();
    RuntimeException error = new RuntimeException(reason);

    for (TestDescriptor test : tests) {
      TestResult testResult = new TestResult(suiteName, test.methodName, false, 0, error);
      failedTests.add(testResult);
      Reporter.printTestResult(suiteName, test.methodName, false, 0, error);
    }

    return failedTests;
  }

  private static List<TestResult> executeTestsInParallel(
      Object instance, List<TestDescriptor> tests, String suiteName) {
    List<Thread> testThreads = new ArrayList<>();
    ConcurrentLinkedQueue<TestResult> testResults = new ConcurrentLinkedQueue<>();

    // Create and start a virtual thread for each test
    for (TestDescriptor test : tests) {
      Thread virtualThread =
          Thread.ofVirtual()
              .name("test-" + suiteName + "#" + test.methodName)
              .start(
                  () -> {
                    TestResult testResult = executeTestInVirtualThread(instance, test, suiteName);
                    testResults.offer(testResult);
                  });

      testThreads.add(virtualThread);
    }

    // Wait for all test threads to complete
    for (Thread thread : testThreads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Test execution was interrupted", e);
      }
    }

    // Convert to list and print results
    List<TestResult> results = new ArrayList<>(testResults);
    for (TestResult testResult : results) {
      Reporter.printTestResult(
          testResult.suiteName,
          testResult.testName,
          testResult.passed,
          testResult.durationMs,
          testResult.error);
    }

    return results;
  }

  private static TestResult executeTestInVirtualThread(
      Object instance, TestDescriptor test, String suiteName) {
    long startTime = System.nanoTime();

    try {
      // Bind the method handle to the instance and invoke
      MethodHandle boundMethod = test.methodHandle.bindTo(instance);
      boundMethod.invokeExact();

      long durationMs = (System.nanoTime() - startTime) / 1_000_000;
      return new TestResult(suiteName, test.methodName, true, durationMs, null);
    } catch (Throwable e) {
      long durationMs = (System.nanoTime() - startTime) / 1_000_000;

      // Extract the actual cause if it's an InvocationTargetException
      Throwable actualError = e instanceof InvocationTargetException ? e.getCause() : e;
      return new TestResult(suiteName, test.methodName, false, durationMs, actualError);
    }
  }

  private static void executeTest(
      Object instance, TestDescriptor test, Result result, String suiteName) {
    result.recordTest();

    long startTime = System.nanoTime();
    boolean passed = true;
    Throwable error = null;

    try {
      // Bind the method handle to the instance and invoke
      MethodHandle boundMethod = test.methodHandle.bindTo(instance);
      boundMethod.invokeExact();
      result.recordPass();
    } catch (Throwable e) {
      passed = false;
      if (e instanceof InvocationTargetException) {
        error = e.getCause();
      } else {
        error = e;
      }
      result.recordFailure(suiteName, test.methodName, error);
    }

    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
    Reporter.printTestResult(suiteName, test.methodName, passed, durationMs, error);
  }
}
