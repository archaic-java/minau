package work.archaic.minau.fixture;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestSuite;
import work.archaic.service.test.v02.TestTrail;

public record RegisteredCases() implements TestSuite {
  public RegisteredCases {
    if (mode().equals("constructor")) throw new IllegalStateException("constructor failed");
  }

  static String mode() { return System.getProperty("fixture", "success"); }

  @Override
  public void cases(Collection<TestCase> cases) {
    switch (mode()) {
      case "constructor" -> throw new AssertionError("unreachable");
      case "registration" -> {
        cases.add(new MustNotRun());
        throw new IllegalArgumentException("registration failed");
      }
      case "null" -> {
        cases.add(new MustNotRun());
        cases.add(null);
      }
      case "description" -> {
        cases.add(new MustNotRun());
        cases.add(new BadDescription());
      }
      case "empty" -> { }
      case "failure" -> cases.add(new Failure());
      case "bounds" -> cases.add(new Bounds());
      case "isolation" -> {
        var started = new CountDownLatch(2);
        cases.add(new IsolatedFailure(1, started));
        cases.add(new IsolatedFailure(2, started));
      }
      default -> {
        var started = new CountDownLatch(2);
        cases.add(new ConcurrentCase(1, started));
        cases.add(new ConcurrentCase(2, started));
        var duplicate = new Duplicate();
        cases.add(duplicate);
        cases.add(duplicate);
        // Intentionally violate the retention rule to check the runner's snapshot isolation.
        cases.add(new MutatesRegistration(cases));
        cases.add(new TrailChecks());
      }
    }
  }
}

record MustNotRun() implements TestCase {
  public void run(TestTrail trail) { System.out.println("PARTIAL_CASE_RAN"); }
}

record BadDescription() implements TestCase {
  public String toString() { throw new IllegalStateException("description failed"); }
  public void run(TestTrail trail) { throw new AssertionError("must not run"); }
}

record ConcurrentCase(int id, CountDownLatch started) implements TestCase {
  public void run(TestTrail trail) throws Exception {
    assert Thread.currentThread().isVirtual();
    started.countDown();
    assert started.await(5, TimeUnit.SECONDS) : "Cases must execute concurrently";
    trail.note("SUCCESS_EVIDENCE_MUST_DISAPPEAR");
  }
}

record Duplicate() implements TestCase {
  public void run(TestTrail trail) { System.out.println("DUPLICATE_EXECUTED"); }
}

record MutatesRegistration(Collection<TestCase> original) implements TestCase {
  public String toString() { return "MutatesRegistration"; }
  public void run(TestTrail trail) { original.clear(); }
}

record TrailChecks() implements TestCase {
  public void run(TestTrail trail) throws Exception {
    try {
      trail.note(null);
      throw new AssertionError("Null note accepted");
    } catch (NullPointerException expected) { }
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(() -> {
        try {
          trail.note("wrong thread");
          throw new AssertionError("Cross-thread note accepted");
        } catch (IllegalStateException expected) { }
      }).get();
    }
    trail.note("SUCCESS_EVIDENCE_MUST_DISAPPEAR");
  }
}

record Failure() implements TestCase {
  public void run(TestTrail trail) {
    trail.note("first observation");
    trail.note("second observation\ncontinued");
    var error = new AssertionError("original assertion");
    error.addSuppressed(new IllegalStateException("cleanup failure"));
    throw error;
  }
}

record Bounds() implements TestCase {
  public void run(TestTrail trail) throws Exception {
    for (int i = 0; i < 260; i++) trail.note("entry-" + i + ":");
    trail.note("x".repeat(2047) + "\uD83D\uDE00");
    throw new java.io.IOException("checked failure");
  }
}

record IsolatedFailure(int id, CountDownLatch started) implements TestCase {
  public String toString() { return "IsolatedFailure[" + id + "]"; }
  public void run(TestTrail trail) throws Exception {
    trail.note("evidence-for-" + id);
    started.countDown();
    assert started.await(5, TimeUnit.SECONDS);
    throw new AssertionError("failure-for-" + id);
  }
}
