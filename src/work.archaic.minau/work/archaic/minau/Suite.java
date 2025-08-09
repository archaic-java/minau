package work.archaic.minau;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import work.archaic.service.test.v01.Test;
import work.archaic.service.test.v01.TestSuite;

final class Suite {
  private final Class<?> suiteClass;
  private final TestSuite instance;
  private final List<TestMethod> testMethods;

  private Suite(Class<?> suiteClass, TestSuite instance, List<TestMethod> testMethods) {
    this.suiteClass = suiteClass;
    this.instance = instance;
    this.testMethods = testMethods;
  }

  static Suite create(Class<?> suiteClass) throws Exception {
    TestSuite instance = (TestSuite) suiteClass.getDeclaredConstructor().newInstance();

    List<TestMethod> testMethods = new ArrayList<>();
    Method[] methods = suiteClass.getDeclaredMethods();

    Arrays.sort(methods, (a, b) -> a.getName().compareTo(b.getName()));

    for (Method method : methods) {
      if (method.isAnnotationPresent(Test.class)
          && !Modifier.isStatic(method.getModifiers())
          && method.getParameterCount() == 0) {

        method.setAccessible(true);
        testMethods.add(new TestMethod(method.getName(), method));
      }
    }

    return new Suite(suiteClass, instance, testMethods);
  }

  void runInto(Result result) {
    result.recordSuite();
    String suiteName = suiteClass.getSimpleName();

    boolean setupSucceeded = true;
    try {
      instance.setup();
    } catch (Throwable e) {
      setupSucceeded = false;
      result.recordSetupFailure(suiteName, e);
    }

    if (setupSucceeded) {
      for (TestMethod testMethod : testMethods) {
        result.recordTest();

        long startTime = System.nanoTime();
        boolean passed = true;
        Throwable error = null;

        try {
          testMethod.method.invoke(instance);
          result.recordPass();
        } catch (Throwable e) {
          passed = false;
          if (e instanceof InvocationTargetException) {
            error = e.getCause();
          } else {
            error = e;
          }
          result.recordFailure(suiteName, testMethod.name, error);
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        Reporter.printTestResult(suiteName, testMethod.name, passed, durationMs, error);
      }
    } else {
      for (TestMethod testMethod : testMethods) {
        result.recordTest();
        Reporter.printTestResult(
            suiteName, testMethod.name, false, 0, new RuntimeException("Setup failed"));
      }
    }

    try {
      instance.teardown();
    } catch (Throwable e) {
      result.recordTeardownFailure(suiteName, e);
    }
  }

  private static class TestMethod {
    final String name;
    final Method method;

    TestMethod(String name, Method method) {
      this.name = name;
      this.method = method;
    }
  }
}
