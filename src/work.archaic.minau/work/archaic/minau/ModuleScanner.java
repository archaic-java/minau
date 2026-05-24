package work.archaic.minau;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.archaic.service.test.v01.Test;
import work.archaic.service.test.v01.TestSuite;

final class ModuleScanner {

  private static final Logger logger = LoggerFactory.getLogger(ModuleScanner.class);
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  /**
   * Scan the given module names, returning all discovered tests.
   *
   * @param moduleNames the modules to scan for tests
   */
  static List<TestDescriptor> discoverTests(Collection<String> moduleNames) throws Exception {
    var discoveredTests = new ArrayList<TestDescriptor>();

    for (String moduleName : moduleNames) {
      // Try to scan classes in the module using available APIs
      scanModuleForTests(moduleName, "out", discoveredTests);
    }

    if (discoveredTests.isEmpty()) {
      logger.warn("No tests found for modules: {}", moduleNames);
    }

    return discoveredTests;
  }

  private static void scanModuleForTests(String moduleName, String outputDir,
    List<TestDescriptor> out) throws Exception {

    var modulePath = Paths.get(outputDir + "/" + moduleName);

    if (!Files.exists(modulePath)) {
      logger.warn("Module output directory not found: {}", modulePath);
      return;
    }

    try (Stream<Path> paths = Files.walk(modulePath)) {
      paths
          .filter(p -> p.toString().endsWith(".class"))
          .filter(p -> !p.getFileName().toString().equals("module-info.class"))
          .forEach(p -> tryLoadClass(modulePath, p, out));
    }
  }

  private static void tryLoadClass(Path moduleRoot, Path classFile, List<TestDescriptor> out) {
    try {
      // Convert file path to class name
      Path relativePath = moduleRoot.relativize(classFile);
      String className =
          relativePath
              .toString()
              .replace(System.getProperty("file.separator"), ".")
              .substring(0, relativePath.toString().length() - 6); // remove .class

      Class<?> c = Class.forName(className, false, ClassLoader.getSystemClassLoader());
      scanClass(c, out);
    } catch (Throwable ignored) {
      // class not loadable / not visible / linkage error → ignore
    }
  }

  private static void scanClass(Class<?> c, List<TestDescriptor> out) {
    // Skip interfaces and abstract classes
    if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
      return;
    }

    // Only scan classes that implement TestSuite
    if (!TestSuite.class.isAssignableFrom(c)) {
      return;
    }

    for (Method m : c.getDeclaredMethods()) {
      if (isTestMethod(m)) {
        try {
          m.setAccessible(true); // requires 'opens' from the test module
          MethodHandle mh = LOOKUP.unreflect(m);
          out.add(new TestDescriptor(c, m.getName(), mh));
        } catch (IllegalAccessException ignored) {
          // If we can't access the method, skip it
        }
      }
    }
  }

  private static boolean isTestMethod(Method m) {
    // Only check for @Test annotation
    return m.isAnnotationPresent(Test.class)
        && m.getParameterCount() == 0
        && m.getReturnType() == void.class
        && !Modifier.isStatic(m.getModifiers());
  }

  private ModuleScanner() {}
}
