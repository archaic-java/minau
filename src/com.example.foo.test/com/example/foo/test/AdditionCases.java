package com.example.foo.test;

import com.example.foo.Calculator;
import java.util.Collection;
import work.archaic.service.test.v02.TestCase;
import work.archaic.service.test.v02.TestSuite;
import work.archaic.service.test.v02.TestTrail;

/** One file assembles data-driven cases and defines their package-private implementations. */
public record AdditionCases() implements TestSuite {
  @Override
  public void cases(Collection<TestCase> cases) {
    cases.add(new Addition(2, 3, 5));
    cases.add(new Addition(-1, 1, 0));
    for (int value = 0; value < 5; value++) {
      cases.add(new Addition(value, 0, value));
    }
  }
}

record Addition(int left, int right, int expected) implements TestCase {
  @Override
  public void run(TestTrail trail) {
    int actual = new Calculator().add(left, right);
    trail.note("Actual sum: " + actual);
    assert actual == expected : "Expected sum: " + expected;
  }
}
