package work.archaic.minau;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

record Options(List<String> modules, boolean debug, boolean list, String suite, Integer ordinal) {
  static Options parse(String... args) {
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

    return new Options(List.copyOf(modulesToTest), debug, list, suite, ordinal);
  }

  ModuleScanner.Discovery select(ModuleScanner.Discovery discovery) {
    if (suite == null) return discovery;
    var legacy = discovery.legacySuites().stream()
        .filter(test -> test.type().getName().equals(suite)).toList();
    var cases = discovery.caseSuites().stream()
        .filter(type -> type.getName().equals(suite)).toList();
    if (legacy.isEmpty() && cases.isEmpty())
      throw new IllegalArgumentException("Unknown suite: " + suite);
    if (ordinal != null && !legacy.isEmpty())
      throw new IllegalArgumentException("--case supports v02 suites only: " + suite);
    return new ModuleScanner.Discovery(legacy, cases);
  }
}
