package work.archaic.minau;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestSuite;

/** Executes registered objects directly, including package-private record implementations. */
final class CaseExecutor {
  static void execute(List<Class<?>> suites, Result result, boolean debug) throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = new ArrayList<Future<Completed>>();
      for (var suiteClass : suites) {
        result.recordSuite();
        var suiteName = suiteClass.getName();
        List<NamedCase> registered;
        try {
          if (!Modifier.isPublic(suiteClass.getModifiers())) {
            throw new IllegalArgumentException("Test suite must be public: " + suiteName);
          }
          var suite = (TestSuite) suiteClass.getConstructor().newInstance();
          var collection = new ArrayList<TestCase>();
          suite.cases(collection);
          var snapshot = List.copyOf(collection);
          registered = new ArrayList<>();
          for (int i = 0; i < snapshot.size(); i++) {
            var test = snapshot.get(i);
            registered.add(new NamedCase("[" + (i + 1) + "] " + test, test));
          }
        } catch (Throwable error) {
          var cause = error instanceof InvocationTargetException wrapped ? wrapped.getCause() : error;
          result.failures++;
          result.failureMessages.add(Reporter.caseFailure(
              suiteName, "registration", cause, new CaseTrail.Evidence(List.of(), 0, 0)));
          continue;
        }
        for (var test : registered) {
          pending.add(executor.submit(() -> run(suiteName, test, debug)));
        }
      }
      for (var future : pending) {
        var completed = future.get();
        result.recordTest();
        result.recordTestDuration(completed.durationMs());
        if (completed.error() == null) {
          result.recordPass();
        } else {
          result.failures++;
          result.failureMessages.add(Reporter.caseFailure(completed.suite(), completed.name(),
              completed.error(), completed.evidence()));
        }
      }
    }
  }

  private static Completed run(String suite, NamedCase named, boolean debug) {
    long start = System.nanoTime();
    var trail = new CaseTrail();
    Throwable failure = null;
    try {
      named.test().run(trail);
    } catch (Throwable error) {
      failure = error;
    }
    var evidence = trail.finish(failure != null);
    long duration = (System.nanoTime() - start) / 1_000_000;
    Reporter.printTestResult(suite, named.name(), failure == null, duration, failure, debug);
    return new Completed(suite, named.name(), duration, failure, evidence);
  }

  private record NamedCase(String name, TestCase test) {}
  private record Completed(String suite, String name, long durationMs, Throwable error,
      CaseTrail.Evidence evidence) {}

  private CaseExecutor() {}
}
