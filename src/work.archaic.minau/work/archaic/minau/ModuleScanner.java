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
import work.archaic.service.test.v01.Test;
import work.archaic.service.test.v01.TestSuite;

final class ModuleScanner {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  record Discovery(List<TestDescriptor> methods, List<Class<?>> caseSuites) {}

  /**
   * Scan the given module names, returning all discovered tests.
   *
   * @param moduleNames the modules to scan for tests
   */
  static Discovery discoverTests(Collection<String> moduleNames) throws Exception {
    var discoveredTests = new ArrayList<TestDescriptor>();
    var caseSuites = new ArrayList<Class<?>>();

    for (String moduleName : moduleNames) {
      // Try to scan classes in the module using available APIs
      scanModuleForTests(moduleName, "out", discoveredTests, caseSuites);
    }

    if (discoveredTests.isEmpty() && caseSuites.isEmpty()) {
      System.out.println("No tests found for modules: " + moduleNames);
    }

    return new Discovery(List.copyOf(discoveredTests), List.copyOf(caseSuites));
  }

  private static void scanModuleForTests(
      String moduleName, String outputDir, List<TestDescriptor> out, List<Class<?>> caseSuites)
      throws Exception {

    var modulePath = Paths.get(outputDir + "/" + moduleName);

    if (!Files.exists(modulePath)) {
      System.out.println("Module output directory not found: " + modulePath);
      return;
    }

    try (Stream<Path> paths = Files.walk(modulePath)) {
      paths
          .filter(p -> p.toString().endsWith(".class"))
          .filter(p -> !p.getFileName().toString().equals("module-info.class"))
          .sorted()
          .forEach(p -> tryLoadClass(modulePath, p, out, caseSuites));
    }
  }

  private static void tryLoadClass(
      Path moduleRoot, Path classFile, List<TestDescriptor> out, List<Class<?>> caseSuites) {
    try {
      // Convert file path to class name
      Path relativePath = moduleRoot.relativize(classFile);
      String className =
          relativePath
              .toString()
              .replace(System.getProperty("file.separator"), ".")
              .substring(0, relativePath.toString().length() - 6); // remove .class

      Class<?> c = Class.forName(className, false, ClassLoader.getSystemClassLoader());
      scanClass(c, out, caseSuites);
    } catch (Throwable ignored) {
      // class not loadable / not visible / linkage error → ignore
    }
  }

  private static void scanClass(Class<?> c, List<TestDescriptor> out, List<Class<?>> caseSuites) {
    // Skip interfaces and abstract classes
    if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
      return;
    }

    if (work.archaic.service.test.v02.TestSuite.class.isAssignableFrom(c)) {
      caseSuites.add(c);
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
