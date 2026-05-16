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

  /*
  * We need to make sure that assertions are enabled.
  * Luckily 'assert' does assignments which we can abuse for this check.
  */
  static {
    var assertEnabled = false;
    assert assertEnabled = true;

    if (assertEnabled != true)
      throw new RuntimeException(
          "The TestRunner expects enabled assertions. Run the JVM with '-ea'.");
  }

  public static void main(String... args) throws Exception {
    if (args.length != 1) showUsageAndExit();

    var modulesToTest = Arrays.stream(args[0].split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());

    var startTimestamp = Instant.now();
    var discoveredTests = ModuleScanner.discoverTests(modulesToTest);
    var testResult = new Result();
    
    TestExecutor.executeTests(discoveredTests, testResult);
    Reporter.printSummary(testResult, Duration.between(startTimestamp, Instant.now()));
    System.exit(testResult.failures == 0 ? 0 : 1);
  }

  private static void showUsageAndExit() {
    String usage =
        """
        Minau - Java Test Runner
        
        Usage:   java -ea -m work.archaic.minau/work.archaic.minau.Main <module>[,<module>...]
        Example: java -ea -m work.archaic.minau/work.archaic.minau.Main com.example.tests
        """;
    logger.info(usage);
    System.exit(2);
  }

}
