package work.archaic.minau;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import work.archaic.service.test.v01.TestSuite;

final class TestExecutor {

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

      for (TestDescriptor test : tests) {
        executeTest(instance, test, result, suiteName);
      }
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
