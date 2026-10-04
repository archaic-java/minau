package work.archaic.minau;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestSuite;

/** Executes registered objects directly, including package-private record implementations. */
final class CaseExecutor {
  static void execute(List<Class<?>> suites, Result result, boolean debug,
      Integer selectedOrdinal, boolean list) throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = new ArrayList<Future<TestOutcome>>();
      for (var suiteClass : suites) {
        result.recordSuite();
        var suiteName = suiteClass.getName();
        List<NamedCase> registered;
        try {
          registered = register(suiteClass);
        } catch (Throwable error) {
          var cause = error instanceof InvocationTargetException wrapped ? wrapped.getCause() : error;
          result.recordFailure(Reporter.caseFailure(
              suiteName, "registration", cause, CaseTrail.Evidence.EMPTY));
          continue;
        }
        if (selectedOrdinal != null && selectedOrdinal > registered.size())
          throw new IllegalArgumentException("--case " + selectedOrdinal + " out of range for "
              + suiteName + " (" + registered.size() + " registrations)");
        if (list) System.out.println(suiteName + " (v02; " + registered.size() + " cases)");
        int from = selectedOrdinal == null ? 0 : selectedOrdinal - 1;
        int to = selectedOrdinal == null ? registered.size() : selectedOrdinal;
        for (int i = from; i < to; i++) {
          var test = registered.get(i);
          if (list)
            System.out.println(suiteName + "#" + Reporter.escape(test.name())
                + "  --suite " + suiteName + " --case " + (i + 1));
          else pending.add(executor.submit(() -> run(suiteName, test, debug)));
        }
      }
      for (var future : pending) result.record(future.get());
    }
  }

  private static List<NamedCase> register(Class<?> suiteClass) throws Exception {
    var suite = (TestSuite) suiteClass.getConstructor().newInstance();
    var collection = new ArrayList<TestCase>();
    suite.cases(collection);
    var snapshot = List.copyOf(collection);
    var named = new ArrayList<NamedCase>();
    for (int i = 0; i < snapshot.size(); i++) {
      var test = snapshot.get(i);
      named.add(new NamedCase("[" + (i + 1) + "] " + test, test));
    }
    return named;
  }

  private static TestOutcome run(String suite, NamedCase named, boolean debug) {
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
    var outcome = new TestOutcome(suite, named.name(), duration, failure, evidence);
    Reporter.printTestResult(outcome, debug);
    return outcome;
  }

  private record NamedCase(String name, TestCase test) {}

  private CaseExecutor() {}
}
