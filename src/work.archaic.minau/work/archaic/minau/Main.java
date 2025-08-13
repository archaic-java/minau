package work.archaic.minau;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  static {
    boolean assertEnabled = false;
    assert assertEnabled = true;
    if (assertEnabled != true)
      throw new RuntimeException(
          "The TestRunner expects enabled assertions. Run the JVM with '-ea'.");
  }

  public static void main(String... args) throws Exception {
    if (args.length == 0) {
      usageAndExit();
    }

    ParsedArgs parsedArgs = parseArgs(args);

    Instant t0 = Instant.now();
    List<TestDescriptor> tests =
        ModuleScanner.discoverTests(parsedArgs.targets, parsedArgs.outputDir);

    Result result = new Result();
    TestExecutor.executeTests(tests, result);

    Duration d = Duration.between(t0, Instant.now());
    Reporter.printSummary(result, d);
    System.exit(result.failures == 0 ? 0 : 1);
  }

  private static ParsedArgs parseArgs(String[] args) {
    String outputDir = "out";
    Set<String> targets = null;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if ("--output-dir".equals(arg)) {
        if (i + 1 < args.length) {
          outputDir = args[++i];
        } else {
          logger.error("Error: --output-dir requires a value");
          usageAndExit();
        }
      } else if (!arg.startsWith("-")) {
        // Assume this is the module list
        targets = parseTargets(arg);
      } else {
        logger.error("Error: Unknown option: {}", arg);
        usageAndExit();
      }
    }

    if (targets == null) {
      logger.error("Error: No modules specified");
      usageAndExit();
    }

    return new ParsedArgs(targets, outputDir);
  }

  private static Set<String> parseTargets(String arg) {
    return Arrays.stream(arg.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private static void usageAndExit() {
    String usage =
        """
        Usage: java -ea -m work.archaic.minau/work.archaic.minau.Main [--output-dir <dir>] <module>[,<module>...]
        Example: java -ea -m work.archaic.minau/work.archaic.minau.Main com.example.tests
                 java -ea -m work.archaic.minau/work.archaic.minau.Main --output-dir build/java com.example.tests\
        """;
    logger.info(usage);
    System.exit(2);
  }

  private static class ParsedArgs {
    final Set<String> targets;
    final String outputDir;

    ParsedArgs(Set<String> targets, String outputDir) {
      this.targets = targets;
      this.outputDir = outputDir;
    }
  }
}
