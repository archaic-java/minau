package work.archaic.minau;

/** Immutable worker result; only the coordinating thread updates the run totals. */
record TestOutcome(String suite, String name, long durationMs, Throwable error,
    CaseTrail.Evidence evidence) {
  TestOutcome(String suite, String name, long durationMs, Throwable error) {
    this(suite, name, durationMs, error, CaseTrail.Evidence.EMPTY);
  }

  boolean passed() { return error == null; }
}
