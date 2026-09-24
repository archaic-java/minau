package work.archaic.minau;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Main {
  static {
    var assertEnabled = false;
    assert assertEnabled = true;
    if (!assertEnabled)
      throw new RuntimeException("The TestRunner expects enabled assertions. Run the JVM with '-ea'.");
  }

  public static void main(String... args) throws Exception {
    try {
      run(args);
    } catch (IllegalArgumentException badUsage) {
      System.err.println("Minau usage error: " + badUsage.getMessage());
      System.err.println(usage());
      System.exit(2);
    }
  }

  private static void run(String... args) throws Exception {
    boolean debug = false;
    boolean list = false;
    String moduleArg = null;
    String suite = null;
    Integer ordinal = null;
    for (int index = 0; index < args.length; index++) {
      String arg = args[index];
      switch (arg) {
        case "--debug" -> {
          if (debug) throw new IllegalArgumentException("Duplicate --debug");
          debug = true;
        }
        case "--list" -> {
          if (list) throw new IllegalArgumentException("Duplicate --list");
          list = true;
        }
        case "--suite" -> {
          if (suite != null) throw new IllegalArgumentException("Duplicate --suite");
          if (++index == args.length || args[index].startsWith("--"))
            throw new IllegalArgumentException("--suite needs a fully qualified class name");
          suite = args[index];
        }
        case "--case" -> {
          if (ordinal != null) throw new IllegalArgumentException("Duplicate --case");
          if (++index == args.length || args[index].startsWith("--"))
            throw new IllegalArgumentException("--case needs a positive registration ordinal");
          try { ordinal = Integer.parseInt(args[index]); }
          catch (NumberFormatException bad) { throw new IllegalArgumentException("Invalid --case ordinal", bad); }
          if (ordinal <= 0) throw new IllegalArgumentException("--case must be positive");
        }
        default -> {
          if (arg.startsWith("--")) throw new IllegalArgumentException("Unknown option: " + arg);
          if (moduleArg != null) throw new IllegalArgumentException("Expected exactly one module list");
          moduleArg = arg;
        }
      }
    }
    if (moduleArg == null || moduleArg.isBlank()) throw new IllegalArgumentException("Missing module list");
    if (ordinal != null && suite == null) throw new IllegalArgumentException("--case requires --suite");
    if (suite != null && suite.isBlank()) throw new IllegalArgumentException("--suite cannot be empty");
    String[] modules = moduleArg.split(",", -1);
    if (Arrays.stream(modules).anyMatch(String::isBlank))
      throw new IllegalArgumentException("Module list contains an empty name");
    Set<String> modulesToTest = new LinkedHashSet<>();
    for (String module : modules) modulesToTest.add(module.strip());

    var startTimestamp = Instant.now();
    var discovered = ModuleScanner.discoverTests(modulesToTest);
    List<TestDescriptor> methods = discovered.methods();
    List<Class<?>> caseSuites = discovered.caseSuites();
    if (suite != null) {
      String selected = suite;
      boolean v01 = methods.stream().anyMatch(test -> test.testClass.getName().equals(selected));
      boolean v02 = caseSuites.stream().anyMatch(type -> type.getName().equals(selected));
      if (!v01 && !v02) throw new IllegalArgumentException("Unknown suite: " + selected);
      if (ordinal != null && v01) throw new IllegalArgumentException("--case supports v02 suites only: " + selected);
      methods = methods.stream().filter(test -> test.testClass.getName().equals(selected)).toList();
      caseSuites = caseSuites.stream().filter(type -> type.getName().equals(selected)).toList();
    }
    var result = new Result();
    if (list) {
      for (var method : methods)
        System.out.println(method.testClass.getName() + "#" + method.methodName + " (v01; --case unavailable)");
    } else TestExecutor.executeTests(methods, result, debug);
    CaseExecutor.execute(caseSuites, result, debug, ordinal, list);
    if (!list || result.failures > 0) Reporter.printSummary(result, Duration.between(startTimestamp, Instant.now()));
    System.exit(result.failures == 0 ? 0 : 1);
  }

  private static String usage() {
    return "Usage: java -ea -m work.archaic.minau/work.archaic.minau.Main "
        + "<module>[,<module>...] [--debug] [--list] [--suite <class>] [--case <ordinal>]";
  }
}
