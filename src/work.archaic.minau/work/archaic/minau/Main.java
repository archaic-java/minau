package work.archaic.minau;

import java.time.Duration;
import java.time.Instant;

public final class Main {
  static {
    var assertEnabled = false;
    assert assertEnabled = true;
    if (!assertEnabled)
      throw new RuntimeException("The TestRunner expects enabled assertions. Run the JVM with '-ea'.");
  }

  public static void main(String... args) throws Exception {
    try {
      run(Options.parse(args));
    } catch (IllegalArgumentException badUsage) {
      System.err.println("Minau usage error: " + badUsage.getMessage());
      System.err.println(usage());
      System.exit(2);
    } catch (ModuleScanner.DiscoveryFailure failure) {
      System.err.println("Minau discovery failed; no tests executed.");
      failure.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static void run(Options options) throws Exception {
    var start = Instant.now();
    var discovery = options.select(ModuleScanner.discoverTests(options.modules()));
    var result = new Result();
    if (options.list()) {
      for (var suite : discovery.legacySuites()) {
        System.out.println(suite.type().getName() + " (v01; " + suite.methods().size() + " tests)");
        for (var method : suite.methods())
          System.out.println(suite.type().getName() + "#" + method.methodName() + " (v01; --case unavailable)");
      }
    } else TestExecutor.execute(discovery.legacySuites(), result, options.debug());
    CaseExecutor.execute(discovery.caseSuites(), result, options.debug(), options.ordinal(), options.list());
    if (!options.list() || result.failures > 0)
      Reporter.printSummary(result, Duration.between(start, Instant.now()));
    System.exit(result.failures == 0 ? 0 : 1);
  }

  private static String usage() {
    return "Usage: java -ea -m work.archaic.minau/work.archaic.minau.Main "
        + "<module>[,<module>...] [--debug] [--list] [--suite <class>] [--case <ordinal>]";
  }
}
