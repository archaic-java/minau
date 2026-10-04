package work.archaic.minau.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Real modular fixtures verify discovery, packaging, JPMS access and v01 lifecycles. */
final class DiscoveryTest {
  static void run() throws Exception {
    try (var workspace = new Workspace()) {
      workspace.compile();
      var exploded = workspace.minau(workspace.classes, "fixture.good");
      assert exploded.exit() == 0 && exploded.output().contains("Tests:      1")
          && exploded.output().contains("CASE_RAN") : "Exploded module must execute outside the project: " + exploded;
      assert !exploded.output().contains("INITIALIZER_RAN")
          : "Discovery must not initialize unrelated classes: " + exploded;
      workspace.jar("fixture.good");
      var packaged = workspace.minau(workspace.jars, "fixture.good");
      assert packaged.exit() == 0 && packaged.output().contains("CASE_RAN")
          && packaged.output().contains("Tests:      1") : "Modular JAR must execute the same case: " + packaged;
      var listing = workspace.minau(workspace.jars, "fixture.good", "--list");
      assert listing.exit() == 0 && listing.output().contains("--case 1")
          && !listing.output().contains("CASE_RAN") : "JAR listing must discover without executing: " + listing;

      for (var module : List.of("fixture.emptycases", "fixture.emptylegacy")) {
        var empty = workspace.minau(workspace.classes, module);
        assert empty.exit() == 0 && empty.output().contains("Suites:     1")
            && empty.output().contains("Tests:      0") : "Empty suite must remain valid: " + empty;
        var list = workspace.minau(workspace.classes, module, "--list");
        assert list.exit() == 0 && list.output().contains(module + ".Cases")
            : "Listing must expose empty suites: " + list;
      }
      var unresolved = workspace.minauResolved(workspace.classes, "fixture.good",
          "fixture.good,fixture.missing");
      assert unresolved.exit() == 1 && unresolved.output().contains("--add-modules fixture.missing")
          && !unresolved.output().contains("CASE_RAN") : "Unresolved target must fail before execution: " + unresolved;

      for (var invalid : List.of("nosuites", "hidden", "constructor", "export", "open", "method", "staticmethod", "parameter", "broken")) {
        String modules = "fixture.good,fixture." + invalid;
        var failed = workspace.minau(workspace.classes, modules);
        assert failed.exit() == 1 && failed.output().contains("fixture." + invalid)
            && failed.output().contains("no tests executed") && !failed.output().contains("CASE_RAN")
            && !failed.output().contains("CONSTRUCTOR_RAN") && !failed.output().contains("REGISTRATION_RAN")
            : "Invalid discovery must prevent every case body: " + failed;
        var list = workspace.minau(workspace.classes, modules, "--list");
        assert list.exit() == 1 && !list.output().contains("--case 1")
            : "Listing must enforce the same discovery validation: " + list;
      }
      var broken = workspace.minau(workspace.classes, "fixture.broken");
      assert broken.output().contains("fixture.broken.Cases") && broken.output().contains("NoClassDefFoundError")
          && broken.output().contains("Missing") : "Linkage failures must retain class and cause: " + broken;
      var badExport = workspace.minau(workspace.classes, "fixture.export");
      assert badExport.output().contains("export fixture.export to work.archaic.minau")
          : "Suite access failure must explain its export requirement: " + badExport;
      var badOpen = workspace.minau(workspace.classes, "fixture.open");
      assert badOpen.output().contains("open fixture.open to work.archaic.minau")
          : "Legacy method access failure must explain its opens requirement: " + badOpen;
      var badConstructor = workspace.minau(workspace.classes, "fixture.constructor");
      assert badConstructor.output().contains("public no-argument constructor")
          : "Invalid constructor must identify the requirement: " + badConstructor;
      var multiple = workspace.minau(workspace.classes, "fixture.nosuites,fixture.constructor");
      assert multiple.exit() == 1 && multiple.output().contains("fixture.nosuites")
          && multiple.output().contains("fixture.constructor") && multiple.output().contains("2 discovery error(s)")
          : "Discovery must report errors across all requested modules: " + multiple;
      var selection = workspace.minau(workspace.classes, "fixture.good,fixture.constructor",
          "--suite", "fixture.good.Cases");
      assert selection.exit() == 1 && !selection.output().contains("CASE_RAN")
          : "Suite selection must not conceal a broken requested module: " + selection;

      var legacy = workspace.minau(workspace.classes, "fixture.legacy");
      assert legacy.exit() == 0 && legacy.output().contains("Tests:      2")
          && legacy.output().contains("SETUP") && legacy.output().contains("TEARDOWN")
          : "Legacy methods must share setup and finish before teardown: " + legacy;
      var failure = workspace.minau(workspace.classes, "fixture.legacyfailure");
      assert failure.exit() == 1 && failure.output().contains("ORIGINAL")
          && failure.output().contains("Suppressed: java.lang.IllegalStateException: CLEANUP")
          && failure.output().contains("TEARDOWN") : "Legacy failure must preserve evidence and tear down: " + failure;
      var setup = workspace.minau(workspace.classes, "fixture.setupfailure");
      assert setup.exit() == 1 && setup.output().contains("SETUP_FAILURE")
          && !setup.output().contains("CASE_RAN") && !setup.output().contains("TEARDOWN_RAN")
          : "Failed v01 setup must retain its established lifecycle: " + setup;
      var teardown = workspace.minau(workspace.classes, "fixture.teardownfailure");
      assert teardown.exit() == 1 && teardown.output().contains("Passed:     1")
          && teardown.output().contains("TEARDOWN_FAILURE") : "Teardown failure must fail a successful case: " + teardown;
    }
    System.out.println("Minau discovery checks passed (modules, JARs, failures and lifecycle)");
  }

  private record Source(String name, String visibility, String version, String body) {}

  private static final List<Source> SOURCES = List.of(
      new Source("good", "exports", "v02", """
          public record Cases() implements TestSuite {
            public Cases { System.out.println("CONSTRUCTOR_RAN"); }
            public void cases(Collection<TestCase> cases) {
              System.out.println("REGISTRATION_RAN");
              cases.add(new One());
            }
          }
          record One() implements TestCase {
            public void run(TestTrail trail) { System.out.println("CASE_RAN"); }
          }
          class Unused {
            static { if (true) throw new AssertionError("INITIALIZER_RAN"); }
          }
          """),
      new Source("emptycases", "exports", "v02", """
          public record Cases() implements TestSuite {
            public void cases(Collection<TestCase> cases) {}
          }
          """),
      new Source("emptylegacy", "opens", "v01", "public record Cases() implements TestSuite {}"),
      new Source("nosuites", "", "v02", "class Cases {}"),
      new Source("hidden", "exports", "v02", """
          record Cases() implements TestSuite {
            public void cases(Collection<TestCase> cases) {}
          }
          """),
      new Source("constructor", "exports", "v02", """
          public record Cases(int value) implements TestSuite {
            public void cases(Collection<TestCase> cases) {}
          }
          """),
      new Source("export", "", "v02", """
          public record Cases() implements TestSuite {
            public void cases(Collection<TestCase> cases) {}
          }
          """),
      new Source("open", "exports", "v01", """
          public record Cases() implements TestSuite { @Test private void hidden() {} }
          """),
      new Source("method", "opens", "v01", """
          public record Cases() implements TestSuite { @Test public int invalid() { return 1; } }
          """),
      new Source("staticmethod", "opens", "v01", """
          public record Cases() implements TestSuite { @Test public static void invalid() {} }
          """),
      new Source("parameter", "opens", "v01", """
          public record Cases() implements TestSuite { @Test public void invalid(int argument) {} }
          """),
      new Source("broken", "exports", "v02", """
          public record Cases() implements TestSuite {
            public void cases(Collection<TestCase> cases) {}
            public Missing missing() { return null; }
          }
          class Missing {}
          class Helper extends Missing {}
          """),
      new Source("legacy", "opens", "v01", """
          public class Cases implements TestSuite {
            private final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(2);
            private volatile boolean ready;
            public void setup() { ready = true; System.out.println("SETUP"); }
            @Test public void first() throws Exception {
              assert Thread.currentThread().isVirtual() : "Legacy methods must run on virtual threads";
              assert ready : "Setup must precede cases";
              started.countDown();
              assert started.await(5, java.util.concurrent.TimeUnit.SECONDS) : "Methods must run concurrently";
            }
            @Test public void second() throws Exception {
              assert ready : "Methods must share the initialized suite";
              started.countDown();
              assert started.await(5, java.util.concurrent.TimeUnit.SECONDS) : "Methods must run concurrently";
            }
            public void teardown() {
              assert started.getCount() == 0 : "Methods must finish before teardown";
              System.out.println("TEARDOWN");
            }
          }
          """),
      new Source("legacyfailure", "opens", "v01", """
          public record Cases() implements TestSuite {
            @Test public void fails() {
              var error = new AssertionError("ORIGINAL");
              error.addSuppressed(new IllegalStateException("CLEANUP"));
              throw error;
            }
            public void teardown() { System.out.println("TEARDOWN"); }
          }
          """),
      new Source("setupfailure", "opens", "v01", """
          public record Cases() implements TestSuite {
            public void setup() { throw new IllegalStateException("SETUP_FAILURE"); }
            @Test public void test() { System.out.println("CASE_RAN"); }
            public void teardown() { System.out.println("TEARDOWN_RAN"); }
          }
          """),
      new Source("teardownfailure", "opens", "v01", """
          public record Cases() implements TestSuite {
            @Test public void test() {}
            public void teardown() { throw new IllegalStateException("TEARDOWN_FAILURE"); }
          }
          """));

  private static final class Workspace implements AutoCloseable {
    private final Path root = Files.createTempDirectory("minau-discovery-");
    private final Path classes = root.resolve("classes");
    private final Path jars = root.resolve("jars");
    private final Path runner = Path.of("out").toAbsolutePath();

    Workspace() throws Exception { Files.createDirectories(jars); }

    void compile() throws Exception {
      Path sources = root.resolve("src");
      for (var source : SOURCES) {
        String module = "fixture." + source.name();
        Path directory = sources.resolve(module);
        Path code = directory.resolve("fixture/" + source.name() + "/Cases.java");
        Files.createDirectories(code.getParent());
        String visibility = source.visibility().isEmpty() ? ""
            : source.visibility() + " " + module + " to work.archaic.minau;";
        Files.writeString(directory.resolve("module-info.java"),
            "module " + module + " { requires work.archaic.service.catalog; " + visibility + " }");
        Files.writeString(code, "package " + module + ";\nimport java.util.Collection;\n"
            + "import work.archaic.service.test." + source.version() + ".*;\n" + source.body());
      }
      String modules = String.join(",", SOURCES.stream().map(source -> "fixture." + source.name()).toList());
      var compile = tool("javac", "--module-source-path", sources.toString(), "--module-path",
          runner.toString(), "-d", classes.toString(), "--module", modules);
      if (compile.exit() != 0) throw new IllegalStateException("Cannot compile discovery fixtures: " + compile);
      Files.delete(classes.resolve("fixture.broken/fixture/broken/Missing.class"));
    }

    void jar(String module) throws Exception {
      var packaged = tool("jar", "--create", "--file", jars.resolve(module + ".jar").toString(),
          "-C", classes.resolve(module).toString(), ".");
      if (packaged.exit() != 0) throw new IllegalStateException("Cannot package fixture: " + packaged);
    }

    Run minau(Path path, String modules, String... flags) throws Exception {
      return minauResolved(path, modules, modules, flags);
    }

    Run minauResolved(Path path, String resolved, String requested, String... flags) throws Exception {
      var args = new ArrayList<>(List.of("-ea", "--module-path", path + ":" + runner,
          "--add-modules", resolved, "-m", "work.archaic.minau/work.archaic.minau.Main", requested));
      args.addAll(List.of(flags));
      return tool("java", args.toArray(String[]::new));
    }

    private Run tool(String executable, String... args) throws Exception {
      var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", executable).toString()));
      command.addAll(List.of(args));
      Path output = Files.createTempFile(root, "process-", ".log");
      var process = new ProcessBuilder(command).directory(root.toFile())
          .redirectErrorStream(true).redirectOutput(output.toFile()).start();
      try {
        process.getOutputStream().close();
        if (!process.waitFor(20, TimeUnit.SECONDS))
          throw new IllegalStateException("Fixture command timed out: " + command);
        return new Run(process.exitValue(), Files.readString(output));
      } finally {
        if (process.isAlive()) {
          process.destroyForcibly();
          process.waitFor(5, TimeUnit.SECONDS);
        }
        Files.deleteIfExists(output);
      }
    }

    public void close() throws Exception {
      try (var paths = Files.walk(root)) {
        for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
  }

  private record Run(int exit, String output) {}
}
