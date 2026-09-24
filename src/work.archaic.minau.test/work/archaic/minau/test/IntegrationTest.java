package work.archaic.minau.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Exercises the real CLI and JPMS boundaries in isolated JVMs. */
public final class IntegrationTest {
  public static void main(String[] args) throws Exception {
    var success = run("success", true, "work.archaic.minau.fixture", true);
    expect(success, 0, "Suites:     1", "Tests:      6", "Passed:     6", "Failed:     0",
        "[3] Duplicate[]", "[4] Duplicate[]");
    assert success.output().split("DUPLICATE_EXECUTED", -1).length == 3 : success;
    assert !success.output().contains("SUCCESS_EVIDENCE_MUST_DISAPPEAR") : success;

    var failure = run("failure", true, "work.archaic.minau.fixture", false);
    expect(failure, 1, "Tests:      1", "Failed:     1", "Failure[]", "first observation",
        "second observation\\ncontinued", "java.lang.AssertionError: original assertion",
        "Suppressed: java.lang.IllegalStateException: cleanup failure", "Failure.run(");
    assert failure.output().indexOf("first observation")
        < failure.output().indexOf("second observation") : failure;

    var bounds = run("bounds", true, "work.archaic.minau.fixture", false);
    expect(bounds, 1, "Trail loss: 5 omitted, 1 truncated", "entry-5:", "entry-259:",
        "java.io.IOException: checked failure");
    assert !bounds.output().contains("entry-4:") : bounds;
    assert !bounds.output().contains("\uFFFD") : bounds;

    for (var mode : new String[] {"constructor", "registration", "null", "description"}) {
      var registration = run(mode, true, "work.archaic.minau.fixture", false);
      expect(registration, 1, "Suites:     1", "Tests:      0", "Failed:     1", "#registration");
      assert !registration.output().contains("PARTIAL_CASE_RAN") : registration;
    }
    expect(run("empty", true, "work.archaic.minau.fixture", false), 0,
        "Suites:     1", "Tests:      0", "Failed:     0");

    // Existing annotated suites and the new record suite are discovered in the same module.
    expect(run("success", true, "com.example.foo.test", false), 0,
        "Suites:     3", "Failed:     0");
    var isolated = run("isolation", true, "work.archaic.minau.fixture", false);
    expect(isolated, 1, "Tests:      2", "Failed:     2", "evidence-for-1", "evidence-for-2");
    int first = isolated.output().indexOf("#[1] IsolatedFailure[1]");
    int second = isolated.output().indexOf("#[2] IsolatedFailure[2]");
    assert first >= 0 && second > first : isolated;
    assert !isolated.output().substring(first, second).contains("evidence-for-2") : isolated;
    assert !isolated.output().substring(second).contains("evidence-for-1") : isolated;
    expect(run("registration", true, "work.archaic.minau.fixture,com.example.foo.test", false), 1,
        "Suites:     4", "Tests:      21", "Passed:     21", "Failed:     1");
    var disabled = run("success", false, "work.archaic.minau.fixture", false);
    assert disabled.exit() != 0 && disabled.output().contains("expects enabled assertions") : disabled;

    String fixture = "work.archaic.minau.fixture.RegisteredCases";
    var listing = run("success", true, "work.archaic.minau.fixture", false, "--list");
    expect(listing, 0, fixture + "#[3] Duplicate[]", fixture + "#[4] Duplicate[]",
        "--suite " + fixture + " --case 3", "--suite " + fixture + " --case 4");
    assert !listing.output().contains("DUPLICATE_EXECUTED") : listing;
    assert !listing.output().contains("SUCCESS_EVIDENCE_MUST_DISAPPEAR") : listing;
    var selected = run("success", true, "work.archaic.minau.fixture", true,
        "--suite", fixture, "--case", "3");
    expect(selected, 0, "Suites:     1", "Tests:      1", "[3] Duplicate[]");
    assert selected.output().split("DUPLICATE_EXECUTED", -1).length == 2 : selected;
    var secondDuplicate = run("success", true, "work.archaic.minau.fixture", true,
        "--suite", fixture, "--case", "4");
    expect(secondDuplicate, 0, "Tests:      1", "[4] Duplicate[]");
    var chosenList = run("success", true, "work.archaic.minau.fixture", false,
        "--list", "--suite", fixture, "--case", "4");
    expect(chosenList, 0, "#[4] Duplicate[]");
    assert !chosenList.output().contains("#[3] Duplicate[]") : chosenList;
    var excluded = run("constructor", true, "work.archaic.minau.fixture,com.example.foo.test",
        false, "--suite", "com.example.foo.test.AdditionCases");
    expect(excluded, 0, "Suites:     1", "Tests:      7", "Failed:     0");
    assert !excluded.output().contains("constructor failed") : excluded;
    var registrationList = run("registration", true, "work.archaic.minau.fixture", false,
        "--list", "--suite", fixture);
    expect(registrationList, 1, "#registration", "Failed:     1");
    assert !registrationList.output().contains("PARTIAL_CASE_RAN") : registrationList;
    expect(run("success", true, "com.example.foo.test", false,
        "--suite", "com.example.foo.test.CalculatorTest"), 0, "Failed:     0");
    for (var flags : new String[][] {
        {"--case", "1"}, {"--suite", fixture, "--case", "0"},
        {"--suite", fixture, "--case", "oops"}, {"--suite", fixture, "--case", "99"},
        {"--suite", "missing.Suite"}, {"--suite"}, {"--case"}, {"--unknown"},
        {"--suite", "com.example.foo.test.CalculatorTest", "--case", "1"}
    }) {
      var module = flags.length > 1 && flags[1].contains("CalculatorTest")
          ? "com.example.foo.test" : "work.archaic.minau.fixture";
      var invalid = run("success", true, module, false, flags);
      expect(invalid, 2, "usage error");
    }
    System.out.println("Minau integration checks passed (selection, listing and legacy CLI)");
  }

  private static void expect(Run run, int exit, String... fragments) {
    assert run.exit() == exit : run;
    for (var fragment : fragments) assert run.output().contains(fragment) : fragment + "\n" + run;
  }

  private static Run run(String mode, boolean assertions, String module, boolean debug,
                         String... flags)
      throws Exception {
    var command = new ArrayList<String>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    if (assertions) command.add("-ea");
    command.add("-Dfixture=" + mode);
    command.addAll(java.util.List.of("--module-path", "out", "--add-modules", module,
        "-m", "work.archaic.minau/work.archaic.minau.Main", module));
    if (debug) command.add("--debug");
    command.addAll(java.util.List.of(flags));
    var output = Files.createTempFile("minau-cli-", ".txt");
    try {
      var process = new ProcessBuilder(command).redirectErrorStream(true)
          .redirectOutput(output.toFile()).start();
      if (!process.waitFor(20, TimeUnit.SECONDS)) {
        process.destroyForcibly().waitFor();
        throw new AssertionError("CLI timed out: " + command);
      }
      return new Run(process.exitValue(), Files.readString(output));
    } finally {
      Files.deleteIfExists(output);
    }
  }

  private record Run(int exit, String output) {}
}
