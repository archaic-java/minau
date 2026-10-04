package work.archaic.minau;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import work.archaic.service.test.v01.TestSuite;

/** Keeps v01's shared suite instance and setup/test/teardown ordering explicit. */
final class TestExecutor {
  private record CompletedSuite(List<TestOutcome> tests, List<String> failures) {}

  static void execute(List<ModuleScanner.LegacySuite> suites, Result result, boolean debug)
      throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = new ArrayList<Future<CompletedSuite>>();
      for (var suite : suites) {
        result.recordSuite();
        pending.add(executor.submit(() -> runSuite(suite, debug)));
      }
      for (var future : pending) {
        var completed = future.get();
        completed.tests().forEach(result::record);
        completed.failures().forEach(result::recordFailure);
      }
    }
  }

  private static CompletedSuite runSuite(ModuleScanner.LegacySuite suite, boolean debug)
      throws Exception {
    String name = suite.type().getName();
    TestSuite instance;
    var outcomes = new ArrayList<TestOutcome>();
    var failures = new ArrayList<String>();
    try {
      instance = (TestSuite) suite.type().getConstructor().newInstance();
      instance.setup();
    } catch (Throwable error) {
      var cause = error instanceof InvocationTargetException wrapped ? wrapped.getCause() : error;
      failures.add(Reporter.caseFailure(name, "setup()", cause, CaseTrail.Evidence.EMPTY));
      for (var method : suite.methods()) {
        var outcome = new TestOutcome(name, method.methodName(), 0, cause);
        outcomes.add(outcome);
        Reporter.printTestResult(outcome, debug);
      }
      return new CompletedSuite(outcomes, failures);
    }
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = new ArrayList<Future<TestOutcome>>();
      for (var method : suite.methods())
        pending.add(executor.submit(() -> runTest(instance, method, name, debug)));
      for (var future : pending) outcomes.add(future.get());
    }
    try {
      instance.teardown();
    } catch (Throwable error) {
      failures.add(Reporter.caseFailure(name, "teardown()", error, CaseTrail.Evidence.EMPTY));
    }
    return new CompletedSuite(outcomes, failures);
  }

  private static TestOutcome runTest(TestSuite instance, TestDescriptor test, String suite, boolean debug) {
    long start = System.nanoTime();
    Throwable failure = null;
    try {
      test.methodHandle().bindTo(instance).invokeExact();
    } catch (Throwable error) {
      failure = error;
    }
    var outcome = new TestOutcome(suite, test.methodName(),
        (System.nanoTime() - start) / 1_000_000, failure);
    Reporter.printTestResult(outcome, debug);
    return outcome;
  }

  private TestExecutor() {}
}
