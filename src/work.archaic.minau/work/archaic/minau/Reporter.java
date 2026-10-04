package work.archaic.minau;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

final class Reporter {

  static String caseFailure(String suite, String name, Throwable error, CaseTrail.Evidence evidence) {
    var text = new StringBuilder(escape(suite)).append("#").append(escape(name));
    for (var note : evidence.notes()) {
      text.append("\n       ").append(escape(note));
    }
    if (evidence.omitted() > 0 || evidence.truncated() > 0) {
      text.append("\n       Trail loss: ").append(evidence.omitted()).append(" omitted, ")
          .append(evidence.truncated()).append(" truncated");
    }
    var stack = new StringWriter();
    error.printStackTrace(new PrintWriter(stack));
    text.append("\n       ").append(stack.toString().stripTrailing().replace("\n", "\n       "));
    return text.toString();
  }

  static String escape(String text) {
    return text.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
  }

  static void printSummary(Result result, Duration duration) {
    double passRate = result.tests() > 0 ? (result.passed * 100.0 / result.tests()) : 0;
    double avgDuration = result.durations.getAverage();
    long minDuration = result.minDuration();
    long maxDuration = result.maxDuration();

    String summary =
        """

        Test Results:
        ├─ Suites:     %d
        ├─ Tests:      %d
        ├─ Passed:     %d (%.0f%%)
        ├─ Failed:     %d
        ├─ Duration:   %d ms (avg: %.1f ms/test)
        └─ Test Times: min: %d ms, max: %d ms\
        """
            .formatted(
                result.suites,
                result.tests(),
                result.passed,
                passRate,
                result.failures,
                duration.toMillis(),
                avgDuration,
                minDuration,
                maxDuration);

    if (!result.failureMessages.isEmpty()) {
      StringBuilder failureSection = new StringBuilder("\n\nFailures:");
      for (String message : result.failureMessages) {
        failureSection.append("\n   • ").append(message);
      }
      summary += failureSection.toString();
    }

    System.out.println(summary);
  }

  static void printTestResult(TestOutcome outcome, boolean debug) {
    if (!debug) return;
    if (outcome.passed())
      System.out.println("Passed '" + escape(outcome.suite()) + "'-suite, test: '" + escape(outcome.name()) + "'");
    else
      System.out.println("✗ " + escape(outcome.suite()) + "#" + escape(outcome.name())
          + " -> " + escape(outcome.error().toString()));
  }
}
