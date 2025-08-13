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

  private record TestResult(String suiteName, String testName, boolean passed, long durationMs, Throwable error) {}

  static void executeTests(List<TestDescriptor> tests, Result result) {
    // Group tests by class to handle TestSuite lifecycle
    Map<Class<?>, List<TestDescriptor>> testsByClass = new HashMap<>();
    for (TestDescriptor test : tests) {
      testsByClass.computeIfAbsent(test.testClass, k -> new ArrayList<>()).add(test);
    }

    for (Map.Entry<Class<?>, List<TestDescriptor>> entry : testsByClass.entrySet()) {
      executeTestsInClass(entry.getKey(), entry.getValue(), result);
    }
  }

  private static void executeTestsInClass(
      Class<?> testClass, List<TestDescriptor> tests, Result result) {
    result.recordSuite();
    String suiteName = testClass.getSimpleName();

    Object instance = null;
    boolean setupSucceeded = true;

    try {
      // Create instance (we know it implements TestSuite)
      instance = testClass.getDeclaredConstructor().newInstance();

      // Run setup
      ((TestSuite) instance).setup();
    } catch (Throwable e) {
      setupSucceeded = false;
      result.recordSetupFailure(suiteName, e);
    }

    if (setupSucceeded && instance != null) {
      // Sort tests by method name for deterministic execution
      tests.sort((a, b) -> a.methodName.compareTo(b.methodName));

      // Execute tests in parallel using virtual threads
      executeTestsInParallel(instance, tests, result, suiteName);
    } else {
      // Mark all tests as failed due to setup failure
      for (TestDescriptor test : tests) {
        result.recordTest();
        Reporter.printTestResult(
            suiteName, test.methodName, false, 0, new RuntimeException("Setup failed"));
      }
    }

    // Run teardown
    if (instance != null) {
      try {
        ((TestSuite) instance).teardown();
      } catch (Throwable e) {
        result.recordTeardownFailure(suiteName, e);
      }
    }
  }

  private static void executeTestsInParallel(
      Object instance, List<TestDescriptor> tests, Result result, String suiteName) {
    List<Thread> testThreads = new ArrayList<>();
    ConcurrentLinkedQueue<TestResult> testResults = new ConcurrentLinkedQueue<>();

    // Create and start a virtual thread for each test
    for (TestDescriptor test : tests) {
      Thread virtualThread = Thread.ofVirtual()
          .name("test-" + suiteName + "#" + test.methodName)
          .start(() -> {
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

    // Process all results after threads complete
    for (TestResult testResult : testResults) {
      result.recordTest();
      result.recordTestDuration(testResult.durationMs);
      if (testResult.passed) {
        result.recordPass();
      } else {
        result.recordFailure(testResult.suiteName, testResult.testName, testResult.error);
      }
      Reporter.printTestResult(testResult.suiteName, testResult.testName, 
                               testResult.passed, testResult.durationMs, testResult.error);
    }
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
